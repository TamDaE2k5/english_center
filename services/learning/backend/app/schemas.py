from datetime import datetime
from decimal import Decimal
from typing import Literal, Optional
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

MaterialType = Literal["DOCUMENT", "VIDEO", "EXERCISE", "SLIDE"]


class MaterialCreate(BaseModel):
    class_id: int = Field(..., gt=0)
    title: str = Field(..., min_length=1, max_length=255)
    description: Optional[str] = None
    material_type: MaterialType = "DOCUMENT"
    file_url: str = Field(...)
    original_filename: Optional[str] = Field(default=None, max_length=255)
    file_size: Optional[int] = Field(default=None, ge=0)

    @field_validator("title")
    @classmethod
    def validate_title(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("Tiêu đề học liệu không được để trống")
        return cleaned

    @field_validator("file_url")
    @classmethod
    def validate_file_url(cls, v: str) -> str:
        cleaned = v.strip() if v is not None else ""
        if not cleaned:
            raise ValueError("LRN_EMPTY_FILE_URL: Đường dẫn file_url không được để trống (INV-LRN-04)")
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
    def validate_assessment_name(cls, v: str) -> str:
        cleaned = v.strip()
        if not cleaned:
            raise ValueError("Tên đầu điểm không được để trống")
        return cleaned

    @model_validator(mode="after")
    def validate_invariants(self):
        if self.max_score <= 0 or self.score < 0 or self.score > self.max_score:
            raise ValueError(
                "LRN_INVALID_SCORE: Điểm số phải thỏa mãn 0 <= score <= max_score và max_score > 0 (INV-LRN-01)"
            )
        if self.weight is not None and (self.weight <= 0 or self.weight > 1):
            raise ValueError(
                "LRN_INVALID_WEIGHT: Trọng số weight phải nằm trong khoảng (0, 1]"
            )
        return self


class GradeUpdate(BaseModel):
    score: Decimal = Field(...)
    max_score: Optional[Decimal] = None
    weight: Optional[Decimal] = None
    feedback: Optional[str] = None

    @model_validator(mode="after")
    def validate_update_fields(self):
        if self.max_score is not None and self.max_score <= 0:
            raise ValueError("LRN_INVALID_SCORE: max_score phải lớn hơn 0 (INV-LRN-01)")
        if self.score < 0:
            raise ValueError("LRN_INVALID_SCORE: Điểm số không được âm (INV-LRN-01)")
        if self.max_score is not None and self.score > self.max_score:
            raise ValueError(
                "LRN_INVALID_SCORE: Điểm số không được vượt quá max_score (INV-LRN-01)"
            )
        if self.weight is not None and (self.weight <= 0 or self.weight > 1):
            raise ValueError(
                "LRN_INVALID_WEIGHT: Trọng số weight phải nằm trong khoảng (0, 1]"
            )
        return self


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