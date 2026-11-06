from pydantic import BaseModel, Field, model_validator
from datetime import datetime
from typing import Optional

class MaterialCreate(BaseModel):
    class_id: int
    title: str
    description: Optional[str] = None
    material_type: str = 'DOCUMENT'
    file_url: Optional[str] = None

class MaterialResponse(MaterialCreate):
    id: int
    uploaded_at: datetime
    class Config:
        from_attributes = True

class GradeCreate(BaseModel):
    class_id: int
    student_id: int
    assessment_name: str
    score: float = Field(..., ge=0, description="Điểm không được âm")
    max_score: float = Field(100.0, gt=0, description="Điểm tối đa phải lớn hơn 0")
    feedback: Optional[str] = None

    @model_validator(mode='after')
    def check_score_range(self) -> 'GradeCreate':
        if self.score > self.max_score:
            raise ValueError('LRN_GRADE_INVALID: Điểm số không được lớn hơn điểm tối đa')
        return self

class GradeResponse(GradeCreate):
    id: int
    graded_at: datetime
    class Config:
        from_attributes = True