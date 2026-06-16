package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    List<CourseReview> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    Optional<CourseReview> findByCourseIdAndUserId(Long courseId, Long userId);

    long countByCourseId(Long courseId);

    @Query("""
            select
                coalesce(avg(r.difficultyRating), 0),
                coalesce(avg(r.workloadRating), 0),
                coalesce(avg(r.gainRating), 0),
                coalesce(avg(r.clarityRating), 0),
                coalesce(avg(r.examPressureRating), 0),
                coalesce(avg(r.recommendRating), 0),
                count(r)
            from CourseReview r
            where r.course.id = :courseId
            """)
    Object[] summarizeByCourseId(@Param("courseId") Long courseId);
}
