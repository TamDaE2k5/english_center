from decimal import Decimal
from sqlalchemy import (
    BigInteger,
    CheckConstraint,
    Column,
    DateTime,
    Index,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
    func,
    text,
)
from .database import Base


class Material(Base):
    __tablename__ = "materials"
    __table_args__ = (
        CheckConstraint(
            "material_type IN ('DOCUMENT', 'VIDEO', 'EXERCISE', 'SLIDE')",
            name="materials_material_type_check",
        ),
        CheckConstraint(
            "btrim(file_url) <> ''",
            name="materials_file_url_check",
        ),
        CheckConstraint(
            "file_size >= 0",
            name="materials_file_size_check",
        ),
        Index("idx_materials_class", "class_id"),
        Index("idx_materials_type", "class_id", "material_type"),
        {"schema": "learning"},
    )

    id = Column(Integer, primary_key=True, index=True)
    class_id = Column(Integer, nullable=False)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    material_type = Column(
        String(50),
        nullable=False,
        default="DOCUMENT",
        server_default=text("'DOCUMENT'"),
    )
    file_url = Column(Text, nullable=False)
    original_filename = Column(String(255), nullable=True)
    file_size = Column(BigInteger, nullable=True)
    uploaded_by = Column(Integer, nullable=True)
    uploaded_at = Column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )
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
        CheckConstraint("score >= 0", name="grades_score_check"),
        CheckConstraint("max_score > 0", name="grades_max_score_check"),
        CheckConstraint("score <= max_score", name="chk_score_lte_max"),
        CheckConstraint(
            "weight IS NULL OR (weight > 0 AND weight <= 1)",
            name="grades_weight_check",
        ),
        Index("idx_grades_class_assessment", "class_id", "assessment_name"),
        Index("idx_grades_class_student", "class_id", "student_id"),
        Index("idx_grades_student", "student_id"),
        {"schema": "learning"},
    )

    id = Column(Integer, primary_key=True, index=True)
    class_id = Column(Integer, nullable=False)
    student_id = Column(Integer, nullable=False)
    assessment_name = Column(String(255), nullable=False)
    score = Column(Numeric(5, 2), nullable=False)
    max_score = Column(
        Numeric(5, 2),
        nullable=False,
        default=Decimal("100.0"),
        server_default=text("100.0"),
    )
    weight = Column(
        Numeric(3, 2),
        nullable=True,
        default=Decimal("1.0"),
        server_default=text("1.0"),
    )
    feedback = Column(Text, nullable=True)
    graded_by = Column(Integer, nullable=True)
    graded_at = Column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )
    updated_at = Column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )