package com.reservation.pfeespace.repository;

import com.reservation.pfeespace.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByDateCreationDesc(Long userId);

    long countByUserIdAndLuFalse(Long userId);
}
