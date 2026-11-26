"""
clients.py - Giao tiep HTTP REST noi bo tu Student Service sang Admission Service.
Tuan thu INV-SYS-02 (khong query cheo schema) va INV-STU-03 (khong tao bang class_members).
Su dung thu vien chuan urllib de giu he thong gon nhe, timeout 5s, retry toi da 3 lan.
"""

import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request
from typing import Any, Dict, List, Optional, Tuple

from fastapi import status

from auth import INTERNAL_SECRET_KEY, StudentException

ADMISSION_SERVICE_URL = os.getenv("ADMISSION_SERVICE_URL", "http://admission-service:8081")
HTTP_TIMEOUT_SECONDS = float(os.getenv("HTTP_TIMEOUT_SECONDS", "5.0"))
HTTP_MAX_RETRIES = int(os.getenv("HTTP_MAX_RETRIES", "3"))


def _http_get_json(
    url: str,
    headers: Optional[Dict[str, str]] = None,
) -> Tuple[Optional[int], Optional[Dict[str, Any]]]:
    """
    Thuc hien HTTP GET toi service khac voi timeout 5 giay va retry toi da 3 lan khi mat ket noi.

    Tham so:
        url: Duong dan day du can goi.
        headers: Tap header bo sung (Authorization hoac X-Internal-Secret).
    Tra ve:
        Tuple (status_code, parsed_json) hoac (None, None) neu khong ket noi duoc.
    """
    req_headers = {"Accept": "application/json"}
    if headers:
        req_headers.update(headers)

    for attempt in range(HTTP_MAX_RETRIES):
        req = urllib.request.Request(url, headers=req_headers, method="GET")
        try:
            with urllib.request.urlopen(req, timeout=HTTP_TIMEOUT_SECONDS) as resp:
                raw_body = resp.read().decode("utf-8")
                return resp.status, (json.loads(raw_body) if raw_body else {})
        except urllib.error.HTTPError as exc:
            raw_body = exc.read().decode("utf-8", errors="ignore")
            try:
                parsed = json.loads(raw_body) if raw_body else {}
            except json.JSONDecodeError:
                parsed = {}
            return exc.code, parsed
        except (urllib.error.URLError, TimeoutError, OSError):
            if attempt < HTTP_MAX_RETRIES - 1:
                time.sleep(0.2 * (2**attempt))
    return None, None


def get_class_info(class_id: int) -> Dict[str, Any]:
    """
    Lay thong tin lop hoc tu Admission Service de kiem tra ton tai va giao vien phu trach.
    Uu tien endpoint internal; neu Admission chua bat internal route thi fallback sang public GET /classes/{id}.

    Tham so:
        class_id: ID lop hoc ben Admission.
    Tra ve:
        Dict thong tin lop hoc (gom id, code, name, teacher_id, status...).
    Ngoai le:
        StudentException(404) neu lop hoc khong ton tai.
        StudentException(503) neu Admission Service khong phan hoi.
    """
    internal_url = f"{ADMISSION_SERVICE_URL}/api/admission/internal/classes/{class_id}"
    status_code, body = _http_get_json(
        internal_url,
        headers={"X-Internal-Secret": INTERNAL_SECRET_KEY},
    )

    if status_code in (None, 403, 404) or (status_code is not None and status_code >= 500):
        public_url = f"{ADMISSION_SERVICE_URL}/api/admission/classes/{class_id}"
        pub_status, pub_body = _http_get_json(public_url)
        if pub_status is not None:
            status_code, body = pub_status, pub_body

    if status_code is None:
        raise StudentException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            code="STU_ADMISSION_UNAVAILABLE",
            message="Khong the ket noi toi Admission Service de xac minh lop hoc",
        )

    if status_code == status.HTTP_404_NOT_FOUND:
        raise StudentException(
            status_code=status.HTTP_404_NOT_FOUND,
            code="STU_CLASS_NOT_FOUND",
            message=f"Khong tim thay lop hoc ID {class_id} ben Admission Service",
        )

    if status_code >= 400:
        raise StudentException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            code="STU_ADMISSION_ERROR",
            message=f"Admission Service tra ve loi HTTP {status_code} khi tra cuu lop hoc",
        )

    data = body.get("data") if isinstance(body, dict) and "data" in body else body
    if not isinstance(data, dict):
        raise StudentException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            code="STU_ADMISSION_ERROR",
            message="Du lieu lop hoc tu Admission Service khong hop le",
        )
    return data


def get_teacher_classes(teacher_user_id: int) -> List[Dict[str, Any]]:
    """
    Lay danh sach tat ca lop hoc ma giao vien co user_id duoc phan cong giang day.

    Tham so:
        teacher_user_id: ID tai khoan giao vien (admission.users.id).
    Tra ve:
        Danh sach dict cac lop hoc co teacher_id == teacher_user_id.
    """
    url = f"{ADMISSION_SERVICE_URL}/api/admission/classes"
    status_code, body = _http_get_json(url)
    if status_code != 200 or not isinstance(body, dict):
        return []
    classes = body.get("data")
    if not isinstance(classes, list):
        return []
    return [
        c for c in classes
        if isinstance(c, dict) and c.get("teacher_id") is not None and int(c["teacher_id"]) == teacher_user_id
    ]


def get_class_approved_enrollments(
    class_id: int,
    raw_token: Optional[str] = None,
) -> List[Dict[str, Any]]:
    """
    Lay danh sach don dang ky APPROVED cua mot lop tu Admission Service (INV-STU-03).

    Tham so:
        class_id: ID lop hoc can lay danh sach hoc vien.
        raw_token: JWT Bearer token cua nguoi goi (dung lam fallback neu internal route chua mo).
    Tra ve:
        Danh sach dict don dang ky da duoc duyet (APPROVED).
    Ngoai le:
        StudentException(502/503) neu Admission Service loi hoac khong ket noi duoc.
    """
    qs = urllib.parse.urlencode({"class_id": class_id, "status": "APPROVED"})
    internal_url = f"{ADMISSION_SERVICE_URL}/api/admission/internal/enrollments?{qs}"
    status_code, body = _http_get_json(
        internal_url,
        headers={"X-Internal-Secret": INTERNAL_SECRET_KEY},
    )

    # Fallback sang public/staff endpoint neu internal route chua duoc Admission khai bao
    if (status_code in (403, 404) or (status_code is not None and status_code >= 500)) and raw_token:
        qs_paged = urllib.parse.urlencode({"class_id": class_id, "status": "APPROVED", "page": 0, "size": 200})
        fallback_url = f"{ADMISSION_SERVICE_URL}/api/admission/enrollments?{qs_paged}"
        fb_status, fb_body = _http_get_json(
            fallback_url,
            headers={"Authorization": f"Bearer {raw_token}"},
        )
        if fb_status is not None and fb_status < 400:
            status_code, body = fb_status, fb_body

    if status_code is None:
        raise StudentException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            code="STU_ADMISSION_UNAVAILABLE",
            message="Khong the ket noi toi Admission Service de lay danh sach lop",
        )

    if status_code >= 400:
        raise StudentException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            code="STU_ADMISSION_ERROR",
            message=f"Admission Service tra ve loi HTTP {status_code} khi lay danh sach dang ky lop",
        )

    data = body.get("data") if isinstance(body, dict) and "data" in body else body
    if isinstance(data, dict):
        items = data.get("content") or data.get("items") or []
    elif isinstance(data, list):
        items = data
    else:
        items = []

    return [
        item for item in items
        if isinstance(item, dict) and str(item.get("status", "APPROVED")).upper() == "APPROVED"
    ]
