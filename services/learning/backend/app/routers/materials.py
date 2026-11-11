from typing import Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session
from ..clients.admission import get_student_enrolled_class_ids, verify_class_and_teacher
from ..database import get_db
from ..deps import AppError, CurrentUser, get_current_user, require_role
from ..models import Material
from ..schemas import MaterialCreate, MaterialResponse

router = APIRouter(prefix="/api/learning/materials", tags=["Materials (LRN-01)"])


@router.get("")
def list_materials(
    class_id: Optional[int] = Query(default=None, gt=0),
    material_type: Optional[str] = Query(default=None),
    search: Optional[str] = Query(default=None),
    page: int = Query(default=1, ge=1),
    size: int = Query(default=20, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    query = db.query(Material)

    # Nếu là STUDENT, kiểm tra chỉ xem học liệu của lớp mình học (nếu Admission trả về danh sách lớp)
    if current_user.role == "STUDENT":
        enrolled_classes = get_student_enrolled_class_ids(current_user.user_id)
        if enrolled_classes is not None:
            if class_id is not None and class_id not in enrolled_classes:
                raise AppError(
                    status.HTTP_403_FORBIDDEN,
                    "LRN_FORBIDDEN_CLASS",
                    "Học viên chỉ được xem học liệu của lớp mình đang theo học",
                )
            if not enrolled_classes:
                return {
                    "success": True,
                    "data": {
                        "items": [],
                        "total": 0,
                        "page": page,
                        "size": size,
                        "total_pages": 0,
                    },
                    "message": "Lấy danh sách học liệu thành công",
                }
            query = query.filter(Material.class_id.in_(enrolled_classes))

    if class_id is not None:
        query = query.filter(Material.class_id == class_id)
    if material_type:
        query = query.filter(Material.material_type == material_type.strip().upper())
    if search and search.strip():
        keyword = f"%{search.strip()}%"
        query = query.filter(Material.title.ilike(keyword))

    total = query.count()
    items = (
        query.order_by(Material.uploaded_at.desc(), Material.id.desc())
        .offset((page - 1) * size)
        .limit(size)
        .all()
    )
    total_pages = (total + size - 1) // size if total > 0 else 0

    return {
        "success": True,
        "data": {
            "items": [
                MaterialResponse.model_validate(item).model_dump(mode="json")
                for item in items
            ],
            "total": total,
            "page": page,
            "size": size,
            "total_pages": total_pages,
        },
        "message": "Lấy danh sách học liệu thành công",
    }


@router.get("/{material_id}")
def get_material_detail(
    material_id: int,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user),
):
    material = db.query(Material).filter(Material.id == material_id).first()
    if not material:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_MATERIAL_NOT_FOUND",
            f"Không tìm thấy học liệu theo ID {material_id}",
        )

    if current_user.role == "STUDENT":
        enrolled_classes = get_student_enrolled_class_ids(current_user.user_id)
        if enrolled_classes is not None and material.class_id not in enrolled_classes:
            raise AppError(
                status.HTTP_403_FORBIDDEN,
                "LRN_FORBIDDEN_CLASS",
                "Học viên chỉ được xem học liệu của lớp mình đang theo học",
            )

    return {
        "success": True,
        "data": MaterialResponse.model_validate(material).model_dump(mode="json"),
        "message": "Lấy chi tiết học liệu thành công",
    }


@router.post("", status_code=status.HTTP_201_CREATED)
def create_material(
    payload: MaterialCreate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_role("ADMIN", "STAFF", "TEACHER")),
):
    if not payload.file_url or not payload.file_url.strip():
        raise AppError(
            status.HTTP_400_BAD_REQUEST,
            "LRN_EMPTY_FILE_URL",
            "Đường dẫn file_url không được để trống (INV-LRN-04)",
        )

    # Xác minh lớp tồn tại ở Admission và nếu là TEACHER thì phải dạy lớp đó (INV-LRN-03)
    verify_class_and_teacher(payload.class_id, current_user)

    material = Material(
        class_id=payload.class_id,
        title=payload.title.strip(),
        description=payload.description,
        material_type=payload.material_type,
        file_url=payload.file_url.strip(),
        original_filename=payload.original_filename,
        file_size=payload.file_size,
        uploaded_by=current_user.user_id,
    )
    db.add(material)
    db.commit()
    db.refresh(material)

    return {
        "success": True,
        "data": MaterialResponse.model_validate(material).model_dump(mode="json"),
        "message": "Đăng tải học liệu thành công",
    }


@router.delete("/{material_id}")
def delete_material(
    material_id: int,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_role("ADMIN", "STAFF", "TEACHER")),
):
    material = db.query(Material).filter(Material.id == material_id).first()
    if not material:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_MATERIAL_NOT_FOUND",
            f"Không tìm thấy học liệu theo ID {material_id}",
        )

    # Nếu là TEACHER phải kiểm tra đúng lớp mình phụ trách (INV-LRN-03)
    if current_user.role == "TEACHER":
        verify_class_and_teacher(material.class_id, current_user)

    db.delete(material)
    db.commit()

    return {
        "success": True,
        "data": {"deleted_id": material_id},
        "message": "Xóa học liệu thành công",
    }