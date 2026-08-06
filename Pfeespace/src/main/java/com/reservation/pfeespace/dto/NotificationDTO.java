package com.reservation.pfeespace.dto;

import java.time.LocalDateTime;

public class NotificationDTO {

    private Long id;
    private String titre;
    private String message;
    private String type;
    private String lien;
    private boolean lu;
    private LocalDateTime dateCreation;

    public NotificationDTO() {
    }

    public NotificationDTO(Long id, String titre, String message, String type,
                           String lien, boolean lu, LocalDateTime dateCreation) {
        this.id = id;
        this.titre = titre;
        this.message = message;
        this.type = type;
        this.lien = lien;
        this.lu = lu;
        this.dateCreation = dateCreation;
    }

    // --- Getters ---
    public Long getId() { return id; }
    public String getTitre() { return titre; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public String getLien() { return lien; }
    public boolean isLu() { return lu; }
    public LocalDateTime getDateCreation() { return dateCreation; }

    // --- Setters ---
    public void setId(Long id) { this.id = id; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setMessage(String message) { this.message = message; }
    public void setType(String type) { this.type = type; }
    public void setLien(String lien) { this.lien = lien; }
    public void setLu(boolean lu) { this.lu = lu; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    // --- Builder simple ---
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final NotificationDTO dto = new NotificationDTO();

        public Builder id(Long id) { dto.setId(id); return this; }
        public Builder titre(String titre) { dto.setTitre(titre); return this; }
        public Builder message(String message) { dto.setMessage(message); return this; }
        public Builder type(String type) { dto.setType(type); return this; }
        public Builder lien(String lien) { dto.setLien(lien); return this; }
        public Builder lu(boolean lu) { dto.setLu(lu); return this; }
        public Builder dateCreation(LocalDateTime dateCreation) { dto.setDateCreation(dateCreation); return this; }

        public NotificationDTO build() {
            return dto;
        }
    }
}