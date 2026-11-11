from typing import Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session
from ..database import get_db
from ..internal_client import verify_class_and_teacher
from ..models import Material
from ..schemas import MaterialCreate, MaterialResponse
from ..security import AppError, CurrentUser, get_current_user, require_roles

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
    if class_id is not None:
        query = query.filter(Material.class_id == class_id)
    if material_type:
        query = query.filter(Material.material_type == material_type.upper())
    if search:
        keyword = f"%{search.strip()}%"
        query = query.filter(Material.title.ilike(keyword))

    total_items = query.count()
    items = (
        query.order_by(Material.uploaded_at.desc(), Material.id.desc())
        .offset((page - 1) * size)
        .limit(size)
        .all()
    )
    total_pages = (total_items + size - 1) // size if total_items > 0 else 1

    return {
        "success": True,
        "data": [MaterialResponse.model_validate(item).model_dump(mode="json") for item in items],
        "pagination": {
            "page": page,
            "size": size,
            "total_items": total_items,
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
            f"Không tìm thấy học liệu với ID {material_id}",
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
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    # Kiểm tra lớp tồn tại và quyền giáo viên phụ trách lớp (INV-LRN-03)
    verify_class_and_teacher(payload.class_id, current_user)

    new_material = Material(
        class_id=payload.class_id,
        title=payload.title,
        description=payload.description,
        material_type=payload.material_type,
        file_url=payload.file_url,
        original_filename=payload.original_filename,
        file_size=payload.file_size,
        uploaded_by=current_user.user_id,
    )
    db.add(new_material)
    db.commit()
    db.refresh(new_material)

    return {
        "success": True,
        "data": MaterialResponse.model_validate(new_material).model_dump(mode="json"),
        "message": "Đăng tải học liệu thành công",
    }


@router.delete("/{material_id}")
def delete_material(
    material_id: int,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(require_roles("ADMIN", "STAFF", "TEACHER")),
):
    material = db.query(Material).filter(Material.id == material_id).first()
    if not material:
        raise AppError(
            status.HTTP_404_NOT_FOUND,
            "LRN_MATERIAL_NOT_FOUND",
            f"Không tìm thấy học liệu với ID {material_id}",
        )

    if current_user.role == "TEACHER":
        verify_class_and_teacher(material.class_id, current_user)

    db.delete(material)
    db.commit()

    return {
        "success": True,
        "data": {"deleted_id": material_id},
        "message": "Xóa học liệu thành công",
    }