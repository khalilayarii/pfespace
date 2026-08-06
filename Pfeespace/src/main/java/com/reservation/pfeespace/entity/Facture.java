// ✅ NOUVEAU FICHIER — Entité Facture
// Chemin : src/main/java/com/reservation/pfeespace/entity/Facture.java

package com.reservation.pfeespace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "factures")
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Numéro unique de facture ex: FAC-2026-0001
    @Column(unique = true)
    private String numero;

    // Relation avec la réservation (1 réservation = 1 facture)
    @OneToOne
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    // Montant calculé (prix salle × durée en heures)
    private double montant;

    // Date de génération
    private LocalDateTime dateGeneration;

    // Chemin du fichier PDF sur le serveur
    private String cheminPdf;

    // Statut : GENEREE, ENVOYEE
    private String statut;

    // --- Getters ---
    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public Reservation getReservation() { return reservation; }
    public double getMontant() { return montant; }
    public LocalDateTime getDateGeneration() { return dateGeneration; }
    public String getCheminPdf() { return cheminPdf; }
    public String getStatut() { return statut; }

    // --- Setters ---
    public void setId(Long id) { this.id = id; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
    public void setMontant(double montant) { this.montant = montant; }
    public void setDateGeneration(LocalDateTime dateGeneration) { this.dateGeneration = dateGeneration; }
    public void setCheminPdf(String cheminPdf) { this.cheminPdf = cheminPdf; }
    public void setStatut(String statut) { this.statut = statut; }
}