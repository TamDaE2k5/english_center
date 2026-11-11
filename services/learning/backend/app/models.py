from sqlalchemy import (
    BigInteger,
    CheckConstraint,
    Column,
    DateTime,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
    func,
)
from .database import Base


class Material(Base):
    __tablename__ = "materials"
    __table_args__ = (
        CheckConstraint(
            "material_type IN ('DOCUMENT', 'VIDEO', 'EXERCISE', 'SLIDE')",
            name="chk_materials_type",
        ),
        CheckConstraint("btrim(file_url) <> ''", name="chk_materials_file_url_not_empty"),
        CheckConstraint("file_size IS NULL OR file_size >= 0", name="chk_materials_file_size"),
        {"schema": "learning"},
    )

    id = Column(Integer, primary_key=True, index=True)
    class_id = Column(Integer, nullable=False, index=True)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    material_type = Column(String(50), nullable=False, default="DOCUMENT")
    file_url = Column(Text, nullable=False)
    original_filename = Column(String(255), nullable=True)
    file_size = Column(BigInteger, nullable=True)
    uploaded_by = Column(Integer, nullable=True)
    uploaded_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )


class Grade(Base):
    __tablename__ = "grades"
    __table_args__ = (
        UniqueConstraint(
            "class_id",
            "student_id",
            "assessment_name",
            name="grades_class_id_student_id_assessment_name_key",
        ),
        CheckConstraint("score >= 0", name="chk_grades_score_non_negative"),
        CheckConstraint("max_score > 0", name="chk_grades_max_score_positive"),
        CheckConstraint("score <= max_score", name="chk_score_lte_max"),
        CheckConstraint(
            "weight IS NULL OR (weight > 0 AND weight <= 1)",
            name="chk_grades_weight_range",
        ),
        {"schema": "learning"},
    )

    id = Column(Integer, primary_key=True, index=True)
    class_id = Column(Integer, nullable=False, index=True)
    student_id = Column(Integer, nullable=False, index=True)
    assessment_name = Column(String(255), nullable=False)
    score = Column(Numeric(5, 2), nullable=False)
    max_score = Column(Numeric(5, 2), nullable=False, default=100.0)
    weight = Column(Numeric(3, 2), nullable=True, default=1.0)
    feedback = Column(Text, nullable=True)
    graded_by = Column(Integer, nullable=True)
    graded_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )