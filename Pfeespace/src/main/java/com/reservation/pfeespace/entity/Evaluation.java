package com.reservation.pfeespace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluations")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(unique = true, nullable = false)
    private String token;

    private Integer noteProprete;          // 1 à 5
    private Integer noteEquipement;        // 1 à 5
    private Integer noteFaciliteReservation; // 1 à 5

    private Boolean capaciteAdaptee;       // true = Oui, false = Non

    @Column(columnDefinition = "TEXT")
    private String problemesRencontres;    // facultatif

    @Column(columnDefinition = "TEXT")
    private String suggestionsAmelioration; // facultatif

    private LocalDateTime dateEnvoi;
    private LocalDateTime dateSoumission;
    private boolean remplie = false;

    // Getters / Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Integer getNoteProprete() { return noteProprete; }
    public void setNoteProprete(Integer noteProprete) { this.noteProprete = noteProprete; }

    public Integer getNoteEquipement() { return noteEquipement; }
    public void setNoteEquipement(Integer noteEquipement) { this.noteEquipement = noteEquipement; }

    public Integer getNoteFaciliteReservation() { return noteFaciliteReservation; }
    public void setNoteFaciliteReservation(Integer n) { this.noteFaciliteReservation = n; }

    public Boolean getCapaciteAdaptee() { return capaciteAdaptee; }
    public void setCapaciteAdaptee(Boolean capaciteAdaptee) { this.capaciteAdaptee = capaciteAdaptee; }

    public String getProblemesRencontres() { return problemesRencontres; }
    public void setProblemesRencontres(String p) { this.problemesRencontres = p; }

    public String getSuggestionsAmelioration() { return suggestionsAmelioration; }
    public void setSuggestionsAmelioration(String s) { this.suggestionsAmelioration = s; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public LocalDateTime getDateSoumission() { return dateSoumission; }
    public void setDateSoumission(LocalDateTime dateSoumission) { this.dateSoumission = dateSoumission; }

    public boolean isRemplie() { return remplie; }
    public void setRemplie(boolean remplie) { this.remplie = remplie; }
}