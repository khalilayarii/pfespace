package com.reservation.pfeespace.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
import com.reservation.pfeespace.entity.Facture;
import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.repository.EntrepriseRepository;
import com.reservation.pfeespace.repository.FactureRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

// ⚠️ LOGOS — déjà placés correctement dans :
// src/main/resources/static/medianet-logo.png
// src/main/resources/static/startupvillage-logo.png
//
// Les logos sont redimensionnés avec scaleToFit(maxWidth, maxHeight), qui force
// l'image dans une boîte maximale tout en gardant ses proportions d'origine.
// Cela évite tout débordement même si l'image source est très large ou très haute.
// Si un fichier est introuvable, un texte de remplacement s'affiche automatiquement (try/catch).

@Service
public class FactureService {

    private final FactureRepository factureRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    // ✅ AJOUT — pour générer/récupérer le token QR de check-in
    private final CheckinService checkinService;

    // ✅ AJOUT — URL du frontend pour construire le lien du QR (ex: http://localhost:4200)
    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private static final String PDF_DIR = "factures/";

    public FactureService(FactureRepository factureRepository,
                          EntrepriseRepository entrepriseRepository,
                          EmailService emailService,
                          NotificationService notificationService,
                          CheckinService checkinService) {
        this.factureRepository = factureRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.checkinService = checkinService;
    }

    public Facture genererFacture(Reservation reservation) throws IOException {

        Optional<Facture> existante = factureRepository.findByReservationId(reservation.getId());
        if (existante.isPresent()) {
            return existante.get();
        }

        new File(PDF_DIR).mkdirs();

        // ✅ AJOUT — on s'assure que la réservation a un token de check-in avant de générer le PDF
        if (reservation.getQrToken() == null) {
            checkinService.genererQrToken(reservation);
        }

        double prixHoraire = reservation.getSalle().getPrix();
        long dureeMinutes = ChronoUnit.MINUTES.between(
                reservation.getHeureDebut(),
                reservation.getHeureFin()
        );
        if (dureeMinutes <= 0) dureeMinutes += 24 * 60;
        double dureeHeures = dureeMinutes / 60.0;
        double montant = prixHoraire * dureeHeures;

        long count = factureRepository.count() + 1;
        String numero = String.format("FAC-%d-%04d",
                LocalDateTime.now().getYear(), count);

        String numFiscal = null;
        if ("INTERNE".equalsIgnoreCase(reservation.getTypeUtilisateur())
                && reservation.getUser() != null
                && reservation.getUser().getEntreprise() != null) {
            numFiscal = reservation.getUser().getEntreprise().getNumFiscal();
        }

        String cheminPdf = PDF_DIR + numero + ".pdf";
        genererPdf(reservation, numero, montant, dureeHeures, prixHoraire, numFiscal, cheminPdf);

        Facture facture = new Facture();
        facture.setNumero(numero);
        facture.setReservation(reservation);
        facture.setMontant(montant);
        facture.setDateGeneration(LocalDateTime.now());
        facture.setCheminPdf(cheminPdf);
        facture.setStatut("GENEREE");

        return factureRepository.save(facture);
    }

    private void genererPdf(Reservation reservation, String numero,
                            double montant, double dureeHeures,
                            double prixHoraire, String numFiscal,
                            String cheminPdf) throws IOException {

        PdfWriter writer = new PdfWriter(cheminPdf);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(40, 50, 40, 50);

        // ===== PALETTE STARTUP VILLAGE / MEDIANET =====
        DeviceRgb bleuSV       = new DeviceRgb(41, 171, 226);
        DeviceRgb orangeSV     = new DeviceRgb(247, 168, 36);
        DeviceRgb rougeRoseSV  = new DeviceRgb(232, 30, 84);
        DeviceRgb vertSV       = new DeviceRgb(141, 198, 63);
        DeviceRgb bleuFonceSV  = new DeviceRgb(31, 41, 61);
        DeviceRgb grisClair    = new DeviceRgb(247, 248, 250);
        DeviceRgb grisTexte    = new DeviceRgb(110, 110, 110);

        PdfFont fontBold = PdfFontFactory.createFont("Helvetica-Bold");
        PdfFont fontNormal = PdfFontFactory.createFont("Helvetica");

        // ── BANDE COULEUR EN HAUT ──
        Table bandeTop = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(15);

        bandeTop.addCell(new Cell().setHeight(5).setBackgroundColor(bleuSV).setBorder(Border.NO_BORDER));
        bandeTop.addCell(new Cell().setHeight(5).setBackgroundColor(orangeSV).setBorder(Border.NO_BORDER));
        bandeTop.addCell(new Cell().setHeight(5).setBackgroundColor(rougeRoseSV).setBorder(Border.NO_BORDER));
        bandeTop.addCell(new Cell().setHeight(5).setBackgroundColor(vertSV).setBorder(Border.NO_BORDER));
        document.add(bandeTop);

        // ── HEADER : Logos (petits, côte à côte, taille contrôlée) + Titre ──
        Table header = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(Border.NO_BORDER);

        Cell logoCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0);

        Table logosTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100));

        // --- Logo Medianet : boîte max 85x30, proportions gardées ---
        Cell medianetCell = new Cell().setBorder(Border.NO_BORDER).setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        try {
            Image logoMedianet = new Image(ImageDataFactory.create(
                    "src/main/resources/static/medianet-logo.png"));
            logoMedianet.scaleToFit(85, 30);
            medianetCell.add(logoMedianet);
        } catch (Exception e) {
            Paragraph mediaTxt = new Paragraph()
                    .add(new Text("MEDIA").setFont(fontBold).setFontSize(14).setFontColor(orangeSV))
                    .add(new Text("NET").setFont(fontBold).setFontSize(14).setFontColor(bleuSV));
            medianetCell.add(mediaTxt);
        }
        logosTable.addCell(medianetCell);

        // --- Logo Startup Village : boîte max 60x45, proportions gardées ---
        Cell svCell = new Cell().setBorder(Border.NO_BORDER).setPadding(2)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        try {
            Image logoSV = new Image(ImageDataFactory.create(
                    "src/main/resources/static/startupvillage-logo.png"));
            logoSV.scaleToFit(60, 45);
            svCell.add(logoSV);
        } catch (Exception e) {
            svCell.add(new Paragraph("STARTUP VILLAGE")
                    .setFont(fontBold).setFontSize(9)
                    .setFontColor(bleuFonceSV));
        }
        logosTable.addCell(svCell);

        logoCell.add(logosTable);
        header.addCell(logoCell);

        // Colonne droite : Titre FACTURE
        Cell titreCell = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
        titreCell.add(new Paragraph("FACTURE")
                .setFont(fontBold).setFontSize(26)
                .setFontColor(bleuFonceSV));
        titreCell.add(new Paragraph(numero)
                .setFont(fontNormal).setFontSize(11)
                .setFontColor(grisTexte));
        titreCell.add(new Paragraph("Date : " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setFont(fontNormal).setFontSize(9)
                .setFontColor(grisTexte));
        header.addCell(titreCell);

        document.add(header);

        // ── Ligne de séparation orange ──
        document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine())
                .setStrokeColor(orangeSV).setMarginTop(12).setMarginBottom(20));

        // ── INFOS CLIENT ──
        document.add(new Paragraph("INFORMATIONS CLIENT")
                .setFont(fontBold).setFontSize(11)
                .setFontColor(bleuSV)
                .setMarginBottom(8));

        Table infoClient = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(grisClair)
                .setMarginBottom(20);

        infoClient.addCell(cellLabel("Nom complet", fontBold, grisTexte));
        infoClient.addCell(cellValue(reservation.getNomComplet(), fontNormal));

        infoClient.addCell(cellLabel("Email", fontBold, grisTexte));
        infoClient.addCell(cellValue(reservation.getMail(), fontNormal));

        infoClient.addCell(cellLabel("Téléphone", fontBold, grisTexte));
        infoClient.addCell(cellValue(reservation.getTelephone(), fontNormal));

        infoClient.addCell(cellLabel("Type", fontBold, grisTexte));
        infoClient.addCell(cellValue(reservation.getTypeUtilisateur(), fontNormal));

        if (numFiscal != null) {
            infoClient.addCell(cellLabel("N° Fiscal", fontBold, grisTexte));
            infoClient.addCell(cellValue(numFiscal, fontNormal));
        }

        document.add(infoClient);

        // ── DÉTAILS RÉSERVATION ──
        document.add(new Paragraph("DÉTAILS DE LA RÉSERVATION")
                .setFont(fontBold).setFontSize(11)
                .setFontColor(rougeRoseSV)
                .setMarginBottom(8));

        Table infoRes = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(grisClair)
                .setMarginBottom(20);

        infoRes.addCell(cellLabel("Salle", fontBold, grisTexte));
        infoRes.addCell(cellValue(reservation.getSalle().getNom(), fontNormal));

        infoRes.addCell(cellLabel("Date", fontBold, grisTexte));
        infoRes.addCell(cellValue(
                reservation.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                fontNormal));

        infoRes.addCell(cellLabel("Heure", fontBold, grisTexte));
        infoRes.addCell(cellValue(
                reservation.getHeureDebut() + " - " + reservation.getHeureFin(),
                fontNormal));

        infoRes.addCell(cellLabel("Nature", fontBold, grisTexte));
        infoRes.addCell(cellValue(reservation.getNatureManifestation(), fontNormal));

        document.add(infoRes);

        // ✅ AJOUT — SECTION CHECK-IN QR CODE
        document.add(new Paragraph("CHECK-IN SUR PLACE")
                .setFont(fontBold).setFontSize(11)
                .setFontColor(vertSV)
                .setMarginBottom(8));

        Table blocQr = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(grisClair)
                .setMarginBottom(20);

        Cell qrCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        try {
            String lienCheckin = frontendUrl + "/checkin/" + reservation.getQrToken();
            byte[] qrBytes = genererQrCodeImage(lienCheckin, 180);
            Image qrImage = new Image(ImageDataFactory.create(qrBytes));
            qrImage.setWidth(90);
            qrImage.setHeight(90);
            qrCell.add(qrImage);
        } catch (Exception e) {
            qrCell.add(new Paragraph("QR indisponible").setFont(fontNormal).setFontSize(9));
        }
        blocQr.addCell(qrCell);

        Cell qrTexteCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        qrTexteCell.add(new Paragraph("Scannez ce QR code avec votre téléphone à votre arrivée pour valider votre présence dans la salle.")
                .setFont(fontNormal).setFontSize(9).setFontColor(grisTexte));
        qrTexteCell.add(new Paragraph("Le check-in doit être effectué pendant votre créneau réservé, sinon la réservation sera automatiquement annulée et la salle libérée.")
                .setFont(fontNormal).setFontSize(9).setFontColor(grisTexte).setMarginTop(4));
        blocQr.addCell(qrTexteCell);

        document.add(blocQr);

        // ── TABLEAU MONTANT ──
        document.add(new Paragraph("DÉTAIL DU MONTANT")
                .setFont(fontBold).setFontSize(11)
                .setFontColor(vertSV)
                .setMarginBottom(8));

        Table tableMontant = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(10);

        for (String h : new String[]{"Description", "Prix/heure", "Durée", "Total"}) {
            tableMontant.addHeaderCell(new Cell()
                    .setBackgroundColor(bleuFonceSV)
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph(h)
                            .setFont(fontBold).setFontSize(10)
                            .setFontColor(ColorConstants.WHITE)
                            .setTextAlignment(TextAlignment.CENTER)));
        }

        tableMontant.addCell(cellMontant("Location - " + reservation.getSalle().getNom(), fontNormal));
        tableMontant.addCell(cellMontant(String.format("%.2f TND", prixHoraire), fontNormal));
        tableMontant.addCell(cellMontant(String.format("%.1f h", dureeHeures), fontNormal));
        tableMontant.addCell(cellMontant(String.format("%.2f TND", montant), fontBold));

        document.add(tableMontant);

        // ── Ligne TOTAL ──
        Table tableTotal = new Table(UnitValue.createPercentArray(new float[]{80, 20}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(30);

        tableTotal.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(grisClair)
                .add(new Paragraph("TOTAL À PAYER")
                        .setFont(fontBold).setFontSize(13)
                        .setFontColor(bleuFonceSV)
                        .setTextAlignment(TextAlignment.RIGHT)));

        tableTotal.addCell(new Cell()
                .setBackgroundColor(orangeSV)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(String.format("%.2f TND", montant))
                        .setFont(fontBold).setFontSize(13)
                        .setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER)));

        document.add(tableTotal);

        // ── BANDE COULEUR EN BAS ──
        Table bandeBottom = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(12);

        bandeBottom.addCell(new Cell().setHeight(4).setBackgroundColor(vertSV).setBorder(Border.NO_BORDER));
        bandeBottom.addCell(new Cell().setHeight(4).setBackgroundColor(rougeRoseSV).setBorder(Border.NO_BORDER));
        bandeBottom.addCell(new Cell().setHeight(4).setBackgroundColor(orangeSV).setBorder(Border.NO_BORDER));
        bandeBottom.addCell(new Cell().setHeight(4).setBackgroundColor(bleuSV).setBorder(Border.NO_BORDER));
        document.add(bandeBottom);

        // ── FOOTER ──
        document.add(new Paragraph("Medianet — Startup Village  |  contact@medianet.tn  |  www.medianet.tn")
                .setFont(fontNormal).setFontSize(9)
                .setFontColor(grisTexte)
                .setTextAlignment(TextAlignment.CENTER));

        document.close();
    }

    // ✅ AJOUT — Génère une image PNG (en bytes) d'un QR code à partir d'un texte/URL
    private byte[] genererQrCodeImage(String texte, int taille) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(texte, BarcodeFormat.QR_CODE, taille, taille);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

    private Cell cellLabel(String text, PdfFont font, DeviceRgb color) {
        return new Cell().setBorder(Border.NO_BORDER)
                .setPadding(6)
                .add(new Paragraph(text).setFont(font).setFontSize(10).setFontColor(color));
    }

    private Cell cellValue(String text, PdfFont font) {
        return new Cell().setBorder(Border.NO_BORDER)
                .setPadding(6)
                .add(new Paragraph(text != null ? text : "-").setFont(font).setFontSize(10));
    }

    private Cell cellMontant(String text, PdfFont font) {
        return new Cell().setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setPadding(6)
                .add(new Paragraph(text).setFont(font).setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER));
    }

    public void envoyerFactureParMail(Long factureId) throws IOException {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));

        emailService.envoyerEmailFacture(
                facture.getReservation().getMail(),
                facture.getReservation().getNomComplet(),
                facture.getNumero(),
                facture.getCheminPdf()
        );

        facture.setStatut("ENVOYEE");
        factureRepository.save(facture);

        // ✅ Notification — seulement si la réservation est liée à un compte User
        if (facture.getReservation().getUser() != null) {
            notificationService.creerNotification(
                    facture.getReservation().getUser(),
                    "Facture envoyée",
                    "Votre facture " + facture.getNumero() + " vous a été envoyée par email.",
                    "FACTURE_ENVOYEE",
                    "/mes-reservations");
        }
    }

    public List<Facture> getAll() {
        return factureRepository.findAllByOrderByDateGenerationDesc();
    }

    public Facture getById(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));
    }

    public Optional<Facture> getByReservation(Long reservationId) {
        return factureRepository.findByReservationId(reservationId);
    }
}