package com.englishcenter.admission.dto.test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO kết quả đề xuất lộ trình và khóa học phù hợp với điểm khảo thí (FR-TST-05, TASK-ADM-32).
 * Tuân thủ quy ước snake_case khi tuần tự hóa sang JSON.
 */
public class RecommendationResultDTO {

    private BigDecimal totalScore;
    private String assessedLevel;
    private RecommendedCourseDTO recommendedCourse;
    private List<UpcomingClassDTO> upcomingClasses;

    public RecommendationResultDTO() {
    }

    public RecommendationResultDTO(BigDecimal totalScore, String assessedLevel,
                                   RecommendedCourseDTO recommendedCourse,
                                   List<UpcomingClassDTO> upcomingClasses) {
        this.totalScore = totalScore;
        this.assessedLevel = assessedLevel;
        this.recommendedCourse = recommendedCourse;
        this.upcomingClasses = upcomingClasses;
    }

    public static class RecommendedCourseDTO {
        private Integer courseId;
        private String courseCode;
        private String title;
        private BigDecimal tuitionFee;
        private String adviceNote;

        public RecommendedCourseDTO() {
        }

        public RecommendedCourseDTO(Integer courseId, String courseCode, String title,
                                    BigDecimal tuitionFee, String adviceNote) {
            this.courseId = courseId;
            this.courseCode = courseCode;
            this.title = title;
            this.tuitionFee = tuitionFee;
            this.adviceNote = adviceNote;
        }

        public Integer getCourseId() {
            return courseId;
        }

        public void setCourseId(Integer courseId) {
            this.courseId = courseId;
        }

        public String getCourseCode() {
            return courseCode;
        }

        public void setCourseCode(String courseCode) {
            this.courseCode = courseCode;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public BigDecimal getTuitionFee() {
            return tuitionFee;
        }

        public void setTuitionFee(BigDecimal tuitionFee) {
            this.tuitionFee = tuitionFee;
        }

        public String getAdviceNote() {
            return adviceNote;
        }

        public void setAdviceNote(String adviceNote) {
            this.adviceNote = adviceNote;
        }
    }

    public static class UpcomingClassDTO {
        private Integer classId;
        private String classCode;
        private String className;
        private LocalDate startDate;
        private Integer availableSlots;

        public UpcomingClassDTO() {
        }

        public UpcomingClassDTO(Integer classId, String classCode, String className,
                                LocalDate startDate, Integer availableSlots) {
            this.classId = classId;
            this.classCode = classCode;
            this.className = className;
            this.startDate = startDate;
            this.availableSlots = availableSlots;
        }

        public Integer getClassId() {
            return classId;
        }

        public void setClassId(Integer classId) {
            this.classId = classId;
        }

        public String getClassCode() {
            return classCode;
        }

        public void setClassCode(String classCode) {
            this.classCode = classCode;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public Integer getAvailableSlots() {
            return availableSlots;
        }

        public void setAvailableSlots(Integer availableSlots) {
            this.availableSlots = availableSlots;
        }
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public String getAssessedLevel() {
        return assessedLevel;
    }

    public void setAssessedLevel(String assessedLevel) {
        this.assessedLevel = assessedLevel;
    }

    public RecommendedCourseDTO getRecommendedCourse() {
        return recommendedCourse;
    }

    public void setRecommendedCourse(RecommendedCourseDTO recommendedCourse) {
        this.recommendedCourse = recommendedCourse;
    }

    public List<UpcomingClassDTO> getUpcomingClasses() {
        return upcomingClasses;
    }

    public void setUpcomingClasses(List<UpcomingClassDTO> upcomingClasses) {
        this.upcomingClasses = upcomingClasses;
    }
}
