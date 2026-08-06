package com.reservation.pfeespace.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "salles")
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String description;
    private int capacite;
    private String equipement;
    private double prix;
    private boolean disponible = true;
    private String image; // ✅ AJOUT

    // ── Getters ──
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getDescription() { return description; }
    public int getCapacite() { return capacite; }
    public String getEquipement() { return equipement; }
    public double getPrix() { return prix; }
    public boolean isDisponible() { return disponible; }
    public String getImage() { return image; } // ✅ AJOUT

    // ── Setters ──
    public void setId(Long id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setDescription(String description) { this.description = description; }
    public void setCapacite(int capacite) { this.capacite = capacite; }
    public void setEquipement(String equipement) { this.equipement = equipement; }
    public void setPrix(double prix) { this.prix = prix; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public void setImage(String image) { this.image = image; } // ✅ AJOUT
}