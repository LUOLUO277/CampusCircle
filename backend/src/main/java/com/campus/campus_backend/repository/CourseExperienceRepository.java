package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.CourseExperience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseExperienceRepository extends JpaRepository<CourseExperience, Long> {
    List<CourseExperience> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    long countByCourseId(Long courseId);

    boolean existsByCourseIdAndUserIdAndSemester(Long courseId, Long userId, String semester);

    @Query("""
            select e from CourseExperience e
            where e.course.id = :courseId
              and (:keyword is null or :keyword = ''
                   or lower(e.studyAdvice) like lower(concat('%', :keyword, '%'))
                   or lower(e.pitfallAdvice) like lower(concat('%', :keyword, '%'))
                   or lower(e.suitableFor) like lower(concat('%', :keyword, '%'))
                   or lower(e.semester) like lower(concat('%', :keyword, '%')))
              and (:semester is null or :semester = '' or e.semester = :semester)
            """)
    Page<CourseExperience> searchPage(@Param("courseId") Long courseId,
            @Param("keyword") String keyword,
            @Param("semester") String semester,
            Pageable pageable);
}
