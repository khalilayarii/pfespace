package com.reservation.pfeespace.dto;

public class SocialLoginRequest {

    private String email;
    private String nom;
    private String provider;
    private String providerId;
    private String token;

    public String getEmail()      { return email; }
    public String getNom()        { return nom; }
    public String getProvider()   { return provider; }
    public String getProviderId() { return providerId; }
    public String getToken()      { return token; }

    public void setEmail(String v)      { this.email = v; }
    public void setNom(String v)        { this.nom = v; }
    public void setProvider(String v)   { this.provider = v; }
    public void setProviderId(String v) { this.providerId = v; }
    public void setToken(String v)      { this.token = v; }
}
