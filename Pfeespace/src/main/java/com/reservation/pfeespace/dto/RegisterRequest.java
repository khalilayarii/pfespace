package com.reservation.pfeespace.dto;

public class RegisterRequest {

    private String nom;
    private String prenom;
    private String email;
    private String mdp;
    private String telephone;

    // ── Getters ─────────────────────────────────
    public String getNom()       { return nom; }
    public String getPrenom()    { return prenom; }
    public String getEmail()     { return email; }
    public String getMdp()       { return mdp; }
    public String getTelephone() { return telephone; }

    // ── Setters ─────────────────────────────────
    public void setNom(String v)       { this.nom = v; }
    public void setPrenom(String v)    { this.prenom = v; }
    public void setEmail(String v)     { this.email = v; }
    public void setMdp(String v)       { this.mdp = v; }
    public void setTelephone(String v) { this.telephone = v; }
}