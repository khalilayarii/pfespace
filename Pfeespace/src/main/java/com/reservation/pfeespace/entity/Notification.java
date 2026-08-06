package com.reservation.pfeespace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String titre;

    @Column(length = 1000)
    private String message;

    // Ex : RESERVATION_CONFIRMEE, RESERVATION_REFUSEE, RESERVATION_ATTENTE,
    // COMPTE_VALIDE, PROFIL_MODIFIE, NOUVELLE_SALLE, FACTURE_ENVOYEE
    private String type;

    // Optionnel : ex "/mes-reservations" pour rediriger au clic
    private String lien;

    private boolean lu = false;

    private LocalDateTime dateCreation;

    public Notification() {
    }

    public Notification(Long id, User user, String titre, String message,
                        String type, String lien, boolean lu, LocalDateTime dateCreation) {
        this.id = id;
        this.user = user;
        this.titre = titre;
        this.message = message;
        this.type = type;
        this.lien = lien;
        this.lu = lu;
        this.dateCreation = dateCreation;
    }

    @PrePersist
    public void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    // --- Getters ---
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getTitre() { return titre; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public String getLien() { return lien; }
    public boolean isLu() { return lu; }
    public LocalDateTime getDateCreation() { return dateCreation; }

    // --- Setters ---
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setMessage(String message) { this.message = message; }
    public void setType(String type) { this.type = type; }
    public void setLien(String lien) { this.lien = lien; }
    public void setLu(boolean lu) { this.lu = lu; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    // --- Builder simple (pour remplacer Notification.builder()...build()) ---
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Notification notification = new Notification();

        public Builder user(User user) { notification.setUser(user); return this; }
        public Builder titre(String titre) { notification.setTitre(titre); return this; }
        public Builder message(String message) { notification.setMessage(message); return this; }
        public Builder type(String type) { notification.setType(type); return this; }
        public Builder lien(String lien) { notification.setLien(lien); return this; }
        public Builder lu(boolean lu) { notification.setLu(lu); return this; }
        public Builder dateCreation(LocalDateTime dateCreation) { notification.setDateCreation(dateCreation); return this; }

        public Notification build() {
            return notification;
        }
    }
}