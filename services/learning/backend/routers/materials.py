from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
import models, schemas
from database import get_db
from auth import get_current_user, require_roles

router = APIRouter(prefix="/api/learning/materials", tags=["Materials"])

@router.get("", response_model=dict)
def get_materials(class_id: int = None, db: Session = Depends(get_db), user: dict = Depends(get_current_user)):
    query = db.query(models.Material)
    if class_id:
        query = query.filter(models.Material.class_id == class_id)
    
    materials = query.all()
    return {"success": True, "data": [schemas.MaterialResponse.model_validate(m) for m in materials]}

@router.post("", response_model=dict, status_code=201)
def create_material(
    material: schemas.MaterialCreate, 
    db: Session = Depends(get_db), 
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "TEACHER"]))
):
    # TODO: Cross-service check (nếu là TEACHER, gọi Admission Service để check xem có dạy class_id này không)
    
    db_material = models.Material(**material.model_dump())
    db.add(db_material)
    db.commit()
    db.refresh(db_material)
    return {"success": True, "data": schemas.MaterialResponse.model_validate(db_material), "message": "Thêm học liệu thành công"}

@router.delete("/{material_id}", status_code=204)
def delete_material(
    material_id: int, 
    db: Session = Depends(get_db), 
    user: dict = Depends(require_roles(["ADMIN", "STAFF", "TEACHER"]))
):
    material = db.query(models.Material).filter(models.Material.id == material_id).first()
    if not material:
        raise HTTPException(status_code=404, detail="LRN_MATERIAL_NOT_FOUND")
    db.delete(material)
    db.commit()
    return None