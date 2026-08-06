package com.reservation.pfeespace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String prenom;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String mdp;

    private String telephone;

    @Column(nullable = false)
    private Boolean actif = true;

    @Enumerated(EnumType.STRING)
    private Role role = Role.CLIENT;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    // ── Constructeur vide obligatoire JPA ───────
    public User() {}

    // ── Builder ─────────────────────────────────
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final User user = new User();

        public Builder nom(String v)            { user.nom = v;         return this; }
        public Builder prenom(String v)         { user.prenom = v;      return this; }
        public Builder email(String v)          { user.email = v;       return this; }
        public Builder mdp(String v)            { user.mdp = v;         return this; }
        public Builder telephone(String v)      { user.telephone = v;   return this; }
        public Builder role(Role v)             { user.role = v;        return this; }
        public Builder actif(Boolean v)         { user.actif = v;       return this; }
        public Builder entreprise(Entreprise v) { user.entreprise = v;  return this; }

        public User build() { return user; }
    }

    // ── Getters ─────────────────────────────────
    public Long getId()                 { return id; }
    public String getNom()              { return nom; }
    public String getPrenom()           { return prenom; }
    public String getEmail()            { return email; }
    public String getMdp()              { return mdp; }
    public String getTelephone()        { return telephone; }
    public Boolean getActif()           { return actif; }
    public Role getRole()               { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Entreprise getEntreprise()   { return entreprise; }

    // ── Setters ─────────────────────────────────
    public void setId(Long id)                  { this.id = id; }
    public void setNom(String v)                { this.nom = v; }
    public void setPrenom(String v)             { this.prenom = v; }
    public void setEmail(String v)              { this.email = v; }
    public void setMdp(String v)                { this.mdp = v; }
    public void setTelephone(String v)          { this.telephone = v; }
    public void setActif(Boolean v)             { this.actif = v; }
    public void setRole(Role v)                 { this.role = v; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }
    public void setEntreprise(Entreprise v)     { this.entreprise = v; }
}