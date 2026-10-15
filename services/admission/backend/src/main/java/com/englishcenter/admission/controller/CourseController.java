package com.englishcenter.admission.controller;

import com.englishcenter.admission.entity.Course;
import com.englishcenter.admission.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admission")
@CrossOrigin(origins = "*")
public class CourseController {

    private final CourseRepository courseRepository;

    public CourseController(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "admission-service",
                "framework", "Spring Boot 3.3.4 (Java 21)"
        ));
    }

    @GetMapping("/courses")
    public ResponseEntity<?> listCourses() {
        List<Course> courses = courseRepository.findAll();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", courses
        ));
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<?> getCourse(@PathVariable Integer id) {
        Optional<Course> courseOpt = courseRepository.findById(id);
        if (courseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "error", Map.of("code", "ADM_COURSE_NOT_FOUND", "message", "Không tìm thấy khóa học với ID: " + id)
            ));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", courseOpt.get()
        ));
    }

    @PostMapping("/courses")
    public ResponseEntity<?> createCourse(@RequestBody Course course) {
        if (course.getCode() == null || course.getName() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", Map.of("code", "ADM_INVALID_COURSE", "message", "Mã khóa học và tên khóa học là bắt buộc")
            ));
        }

        if (courseRepository.findByCode(course.getCode()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "error", Map.of("code", "ADM_COURSE_CODE_EXISTS", "message", "Mã khóa học đã tồn tại")
            ));
        }

        Course saved = courseRepository.save(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Tạo khóa học thành công",
                "data", saved
        ));
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable Integer id, @RequestBody Course updated) {
        return courseRepository.findById(id).map(course -> {
            if (updated.getName() != null) course.setName(updated.getName());
            if (updated.getDescription() != null) course.setDescription(updated.getDescription());
            if (updated.getLevel() != null) course.setLevel(updated.getLevel());
            if (updated.getDurationWeeks() != null) course.setDurationWeeks(updated.getDurationWeeks());
            if (updated.getTuitionAmount() != null) course.setTuitionAmount(updated.getTuitionAmount());
            if (updated.getIsActive() != null) course.setIsActive(updated.getIsActive());

            Course saved = courseRepository.save(course);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật khóa học thành công",
                    "data", saved
            ));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "error", Map.of("code", "ADM_COURSE_NOT_FOUND", "message", "Không tìm thấy khóa học")
        )));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Integer id) {
        if (!courseRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "error", Map.of("code", "ADM_COURSE_NOT_FOUND", "message", "Không tìm thấy khóa học")
            ));
        }
        courseRepository.deleteById(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xóa khóa học thành công"
        ));
    }
}
