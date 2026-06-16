package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Course;
import com.campus.campus_backend.domain.CourseCircleMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseCircleMemberRepository extends JpaRepository<CourseCircleMember, Long> {
    boolean existsByCourseIdAndUserId(Long courseId, Long userId);

    Optional<CourseCircleMember> findByCourseIdAndUserId(Long courseId, Long userId);

    long countByCourseId(Long courseId);

    List<CourseCircleMember> findByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteByCourse(Course course);
}
