package com.reservation.pfeespace.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relation avec l'utilisateur
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Relation avec la salle
    @ManyToOne
    @JoinColumn(name = "salle_id")
    private Salle salle;

    // Étape 1 : infos utilisateur
    private String typeUtilisateur;      // VILLEGIATURE, MANAGER, ADMIN
    private String natureManifestation;  // EVENEMENT, BUREAU, REUNION, COWORKING

    // Étape 2 : infos réservation
    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String mail;
    private String nomComplet;
    private String societe;
    private String telephone;
    private String description;

    // Statut
    @Column(nullable = false)
    private String statut = "EN_ATTENTE"; // EN_ATTENTE, CONFIRMEE, REFUSEE, EN_COURS, TERMINEE, NO_SHOW

    // --- Check-in / Check-out ---
    @Column(unique = true)
    private String qrToken;

    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    // --- Getters ---
    public Long getId() { return id; }
    public User getUser() { return user; }
    public Salle getSalle() { return salle; }
    public String getTypeUtilisateur() { return typeUtilisateur; }
    public String getNatureManifestation() { return natureManifestation; }
    public LocalDate getDate() { return date; }
    public LocalTime getHeureDebut() { return heureDebut; }
    public LocalTime getHeureFin() { return heureFin; }
    public String getMail() { return mail; }
    public String getNomComplet() { return nomComplet; }
    public String getSociete() { return societe; }
    public String getTelephone() { return telephone; }
    public String getDescription() { return description; }
    public String getStatut() { return statut; }
    public String getQrToken() { return qrToken; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }

    // --- Setters ---
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setSalle(Salle salle) { this.salle = salle; }
    public void setTypeUtilisateur(String typeUtilisateur) { this.typeUtilisateur = typeUtilisateur; }
    public void setNatureManifestation(String natureManifestation) { this.natureManifestation = natureManifestation; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }
    public void setMail(String mail) { this.mail = mail; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }
    public void setSociete(String societe) { this.societe = societe; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public void setDescription(String description) { this.description = description; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
}