package com.reservation.pfeespace.dto;

public class LoginRequest {

    private String email;
    private String mdp;

    // ── Getters ─────────────────────────────────
    public String getEmail() { return email; }
    public String getMdp()   { return mdp; }

    // ── Setters ─────────────────────────────────
    public void setEmail(String v) { this.email = v; }
    public void setMdp(String v)   { this.mdp = v; }
}