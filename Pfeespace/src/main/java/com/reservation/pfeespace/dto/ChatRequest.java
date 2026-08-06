package com.reservation.pfeespace.dto;

public class ChatRequest {
    private String message;
    private String userEmail;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}