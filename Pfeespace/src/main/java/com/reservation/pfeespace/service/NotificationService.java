package com.reservation.pfeespace.service;

import com.reservation.pfeespace.dto.NotificationDTO;
import com.reservation.pfeespace.entity.Notification;
import com.reservation.pfeespace.entity.Role;
import com.reservation.pfeespace.entity.User;
import com.reservation.pfeespace.repository.NotificationRepository;
import com.reservation.pfeespace.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ✅ Appelée depuis les autres services (Reservation, User, Salle...)
    public void creerNotification(User user, String titre, String message,
                                  String type, String lien) {
        if (user == null) return;

        Notification notif = Notification.builder()
                .user(user)
                .titre(titre)
                .message(message)
                .type(type)
                .lien(lien)
                .lu(false)
                .build();

        notificationRepository.save(notif);
    }

    public void creerNotificationParEmail(String email, String titre, String message,
                                          String type, String lien) {
        userRepository.findByEmail(email)
                .ifPresent(user -> creerNotification(user, titre, message, type, lien));
    }

    public void notifierTousLesClients(String titre, String message, String type, String lien) {
        List<User> tousLesUsers = userRepository.findAll();
        for (User u : tousLesUsers) {
            if (u.getActif() != null && u.getActif()) {
                creerNotification(u, titre, message, type, lien);
            }
        }
    }

    // ✅ Notifier tous les admins (sans push temps réel)
    public void notifierAdmins(String titre, String message, String type, String lien) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            creerNotification(admin, titre, message, type, lien);
        }
    }

    public List<NotificationDTO> getNotifications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        return notificationRepository.findByUserIdOrderByDateCreationDesc(user.getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public long countNonLues(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return notificationRepository.countByUserIdAndLuFalse(user.getId());
    }

    public void marquerCommeLue(Long id) {
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification introuvable"));
        notif.setLu(true);
        notificationRepository.save(notif);
    }

    public void marquerToutesCommeLues(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        List<Notification> notifs = notificationRepository.findByUserIdOrderByDateCreationDesc(user.getId());
        notifs.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(notifs);
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .titre(n.getTitre())
                .message(n.getMessage())
                .type(n.getType())
                .lien(n.getLien())
                .lu(n.isLu())
                .dateCreation(n.getDateCreation())
                .build();
    }
}