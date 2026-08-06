package com.reservation.pfeespace.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("mailTaskExecutor")
    public void envoyerEmailReset(String toEmail, String token, String resetUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Réinitialisation de votre mot de passe - PfeSpace");
        message.setText(
                "Bonjour,\n\nVous avez demandé la réinitialisation de votre mot de passe.\n\n" +
                        "Cliquez sur ce lien :\n" + resetUrl + "?token=" + token + "\n\n" +
                        "Ce lien est valable 15 minutes.\n\nL'équipe PfeSpace");
        mailSender.send(message);
    }

    @Async("mailTaskExecutor")
    public void envoyerEmailConfirmation(String toEmail, String nomComplet,
                                         String salle, String date,
                                         String heureDebut, String heureFin) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("✅ Réservation confirmée - PfeSpace");
        message.setText(
                "Bonjour " + nomComplet + ",\n\nVotre réservation a été CONFIRMÉE !\n\n" +
                        "📍 Salle : " + salle + "\n📅 Date : " + date + "\n" +
                        "⏰ Heure : " + heureDebut + " - " + heureFin + "\n\n" +
                        "Vous recevrez votre facture séparément.\n\nL'équipe PfeSpace");
        mailSender.send(message);
    }

    @Async("mailTaskExecutor")
    public void envoyerEmailRefus(String toEmail, String nomComplet,
                                  String salle, String date) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("❌ Réservation refusée - PfeSpace");
        message.setText(
                "Bonjour " + nomComplet + ",\n\nVotre réservation a été REFUSÉE.\n\n" +
                        "📍 Salle : " + salle + "\n📅 Date : " + date + "\n\n" +
                        "Pour plus d'informations, contactez-nous.\n\nL'équipe PfeSpace");
        mailSender.send(message);
    }

    @Async("mailTaskExecutor")
    public void envoyerEmailAttente(String toEmail, String nomComplet,
                                    String salle, String date) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("⏳ Réservation en attente - PfeSpace");
        message.setText(
                "Bonjour " + nomComplet + ",\n\nVotre réservation est EN ATTENTE.\n\n" +
                        "📍 Salle : " + salle + "\n📅 Date : " + date + "\n\n" +
                        "Vous recevrez un email dès traitement.\n\nL'équipe PfeSpace");
        mailSender.send(message);
    }

    @Async("mailTaskExecutor")
    public void envoyerEmailEnAttenteValidation(String toEmail, String nomComplet) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("⏳ Votre compte est en cours de validation - PfeSpace");
        message.setText(
                "Bonjour " + nomComplet + ",\n\n" +
                        "Merci de vous être inscrit sur PfeSpace (Startup Village - Medianet).\n\n" +
                        "Votre compte est actuellement EN COURS DE VALIDATION par notre équipe d'administration.\n" +
                        "Vous recevrez un email de confirmation dès que votre compte sera activé.\n\n" +
                        "Merci de votre patience.\n\nL'équipe PfeSpace");
        mailSender.send(message);
    }

    @Async("mailTaskExecutor")
    public void envoyerEmailCompteValide(String toEmail, String nomComplet) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("✅ Votre compte a été validé - PfeSpace");
        message.setText(
                "Bonjour " + nomComplet + ",\n\n" +
                        "Bonne nouvelle ! Votre compte PfeSpace a été VALIDÉ par notre équipe d'administration.\n\n" +
                        "Vous pouvez désormais vous connecter et effectuer vos réservations de salles.\n\n" +
                        "L'équipe PfeSpace — Startup Village Medianet");
        mailSender.send(message);
    }

    @Async("mailTaskExecutor")
    public void envoyerEmailFacture(String toEmail, String nomComplet,
                                    String numeroFacture, String cheminPdf) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🧾 Votre facture " + numeroFacture + " - PfeSpace");
            helper.setText(
                    "Bonjour " + nomComplet + ",\n\n" +
                            "Veuillez trouver en pièce jointe votre facture " + numeroFacture + ".\n\n" +
                            "Merci de votre confiance.\n\nL'équipe PfeSpace — Medianet");

            FileSystemResource file = new FileSystemResource(new File(cheminPdf));
            helper.addAttachment(numeroFacture + ".pdf", file);

            mailSender.send(message);
        } catch (MessagingException e) {
            // ⚠️ Comme la méthode est @Async, cette exception ne remonte plus
            // à l'appelant. On la log ici pour ne pas la perdre silencieusement.
            System.err.println("Erreur envoi email facture (async) : " + e.getMessage());
        }
    }

    @Async("mailTaskExecutor")
    public void envoyerEmailEvaluation(String toEmail, String nomComplet,
                                       String salle, String lienEvaluation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("⭐ Votre avis compte - PfeSpace");
        message.setText(
                "Bonjour " + nomComplet + ",\n\n" +
                        "Votre réservation pour la salle " + salle + " est terminée.\n\n" +
                        "Merci de prendre une minute pour nous donner votre avis :\n" +
                        lienEvaluation + "\n\n" +
                        "Votre retour nous aide à améliorer nos services.\n\nL'équipe PfeSpace");
        mailSender.send(message);
    }
}