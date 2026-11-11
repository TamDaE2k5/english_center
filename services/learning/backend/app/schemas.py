from datetime import datetime
from decimal import Decimal
from typing import Optional, Literal
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

MaterialType = Literal["DOCUMENT", "VIDEO", "EXERCISE", "SLIDE"]


class MaterialCreate(BaseModel):
    class_id: int = Field(..., gt=0)
    title: str = Field(..., min_length=1, max_length=255)
    description: Optional[str] = None
    material_type: MaterialType = "DOCUMENT"
    file_url: str = Field(..., min_length=1)
    original_filename: Optional[str] = Field(None, max_length=255)
    file_size: Optional[int] = Field(None, ge=0)

    @field_validator("title", "file_url")
    @classmethod
    def must_not_be_blank(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("Trường này không được để trống (INV-LRN-04)")
        return cleaned


class MaterialResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    class_id: int
    title: str
    description: Optional[str] = None
    material_type: str
    file_url: str
    original_filename: Optional[str] = None
    file_size: Optional[int] = None
    uploaded_by: Optional[int] = None
    uploaded_at: datetime
    updated_at: datetime


class GradeCreate(BaseModel):
    class_id: int = Field(..., gt=0)
    student_id: int = Field(..., gt=0)
    assessment_name: str = Field(..., min_length=1, max_length=255)
    score: Decimal = Field(...)
    max_score: Decimal = Field(default=Decimal("100.0"))
    weight: Optional[Decimal] = Field(default=Decimal("1.0"))
    feedback: Optional[str] = None

    @field_validator("assessment_name")
    @classmethod
    def clean_assessment_name(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("Tên đầu điểm không được để trống")
        return cleaned

    @model_validator(mode="after")
    def validate_score_and_weight(self):
        if self.max_score <= 0:
            raise ValueError("max_score phải lớn hơn 0 (INV-LRN-01)")
        if self.score < 0 or self.score > self.max_score:
            raise ValueError("Điểm số phải thỏa mãn 0 <= score <= max_score (INV-LRN-01)")
        if self.weight is not None and (self.weight <= 0 or self.weight > 1):
            raise ValueError("Trọng số weight phải nằm trong khoảng (0, 1]")
        return self


class GradeUpdate(BaseModel):
    score: Decimal = Field(...)
    max_score: Optional[Decimal] = None
    weight: Optional[Decimal] = None
    feedback: Optional[str] = None


class GradeResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    class_id: int
    student_id: int
    assessment_name: str
    score: float
    max_score: float
    weight: Optional[float] = 1.0
    feedback: Optional[str] = None
    graded_by: Optional[int] = None
    graded_at: datetime
    updated_at: datetime


class StudentGradeSummaryItem(BaseModel):
    class_id: int
    student_id: int
    total_assessments: int
    weighted_avg_score: float
    average_percentage: float
    result: str


class ClassPassRateSummary(BaseModel):
    class_id: int
    total_students: int
    pass_count: int
    fail_count: int
    pass_rate_percentage: float
    student_summaries: list[StudentGradeSummaryItem] = []