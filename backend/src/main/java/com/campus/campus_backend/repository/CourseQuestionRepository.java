package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.CourseQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseQuestionRepository extends JpaRepository<CourseQuestion, Long> {
    List<CourseQuestion> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    long countByCourseId(Long courseId);

    boolean existsByCourseIdAndTitle(Long courseId, String title);

    @Query("""
            select q from CourseQuestion q
            where q.course.id = :courseId
              and (:keyword is null or :keyword = ''
                   or lower(q.title) like lower(concat('%', :keyword, '%'))
                   or lower(q.content) like lower(concat('%', :keyword, '%')))
              and (:resolved is null or q.resolved = :resolved)
            """)
    Page<CourseQuestion> searchPage(@Param("courseId") Long courseId,
            @Param("keyword") String keyword,
            @Param("resolved") Boolean resolved,
            Pageable pageable);
}
