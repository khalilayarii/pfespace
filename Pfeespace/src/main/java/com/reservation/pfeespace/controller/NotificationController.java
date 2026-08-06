package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.dto.NotificationDTO;
import com.reservation.pfeespace.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationDTO> getNotifications(Authentication auth) {
        return notificationService.getNotifications(auth.getName());
    }

    @GetMapping("/count")
    public Map<String, Long> countNonLues(Authentication auth) {
        return Map.of("count", notificationService.countNonLues(auth.getName()));
    }

    @PutMapping("/{id}/lire")
    public void marquerCommeLue(@PathVariable Long id) {
        notificationService.marquerCommeLue(id);
    }

    @PutMapping("/lire-tout")
    public void marquerToutesCommeLues(Authentication auth) {
        notificationService.marquerToutesCommeLues(auth.getName());
    }
}