package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("""
            select c from Course c
            where (:keyword is null or :keyword = ''
                or lower(c.name) like lower(concat('%', :keyword, '%'))
                or lower(c.teacherName) like lower(concat('%', :keyword, '%'))
                or lower(c.courseCode) like lower(concat('%', :keyword, '%')))
            order by c.memberCount desc, c.name asc
            """)
    List<Course> search(@Param("keyword") String keyword);

    List<Course> findTop6ByOrderByMemberCountDescQuestionCountDescReviewCountDesc();

    Optional<Course> findByCourseCodeIgnoreCase(String courseCode);

    @Query("""
            select c from Course c
            where (:keyword is null or :keyword = ''
                or lower(c.name) like lower(concat('%', :keyword, '%'))
                or lower(c.teacherName) like lower(concat('%', :keyword, '%'))
                or lower(c.courseCode) like lower(concat('%', :keyword, '%')))
            """)
    Page<Course> searchPage(@Param("keyword") String keyword, Pageable pageable);
}
