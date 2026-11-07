from sqlalchemy import Column, Integer, String, Numeric, DateTime, Text
from sqlalchemy.sql import func
from database import Base

class Material(Base):
    __tablename__ = "materials"
    __table_args__ = {"schema": "learning"}

    id = Column(Integer, primary_key=True, index=True)
    class_id = Column(Integer, nullable=False)
    title = Column(String(255), nullable=False)
    description = Column(Text)
    material_type = Column(String(50), default='DOCUMENT')
    file_url = Column(Text)
    uploaded_at = Column(DateTime(timezone=True), server_default=func.now())

class Grade(Base):
    __tablename__ = "grades"
    __table_args__ = {"schema": "learning"}

    id = Column(Integer, primary_key=True, index=True)
    class_id = Column(Integer, nullable=False)
    student_id = Column(Integer, nullable=False)
    assessment_name = Column(String(255), nullable=False)
    score = Column(Numeric(5, 2), nullable=False)
    max_score = Column(Numeric(5, 2), default=100.0)
    feedback = Column(Text)
    graded_at = Column(DateTime(timezone=True), server_default=func.now())