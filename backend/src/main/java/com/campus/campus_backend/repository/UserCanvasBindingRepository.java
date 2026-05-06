package com.campus.campus_backend.repository;

import com.campus.campus_backend.domain.UserCanvasBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCanvasBindingRepository extends JpaRepository<UserCanvasBinding, Long> {
    Optional<UserCanvasBinding> findByUserId(Long userId);

    List<UserCanvasBinding> findByStatusOrderByUpdatedAtDesc(String status);
}
