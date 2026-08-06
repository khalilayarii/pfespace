package com.reservation.pfeespace.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "entreprise")
public class Entreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "num_fiscal", unique = true, nullable = false)
    private String numFiscal;

    private String adresse;
    private String email;
    private String telephone;

    @Column(name = "est_membre")
    private Boolean estMembre = false;

    public Entreprise() {}

    // Getters
    public Long getId()          { return id; }
    public String getNom()       { return nom; }
    public String getNumFiscal() { return numFiscal; }
    public String getAdresse()   { return adresse; }
    public String getEmail()     { return email; }
    public String getTelephone() { return telephone; }
    public Boolean getEstMembre(){ return estMembre; }

    // Setters
    public void setId(Long id)           { this.id = id; }
    public void setNom(String v)         { this.nom = v; }
    public void setNumFiscal(String v)   { this.numFiscal = v; }
    public void setAdresse(String v)     { this.adresse = v; }
    public void setEmail(String v)       { this.email = v; }
    public void setTelephone(String v)   { this.telephone = v; }
    public void setEstMembre(Boolean v)  { this.estMembre = v; }
}