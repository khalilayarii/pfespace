package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.entity.Facture;
import com.reservation.pfeespace.service.FactureService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/factures")
@CrossOrigin(origins = "http://localhost:4200")
public class FactureController {

    private final FactureService factureService;

    public FactureController(FactureService factureService) {
        this.factureService = factureService;
    }

    // GET toutes les factures
    @GetMapping
    public List<Facture> getAll() {
        return factureService.getAll();
    }

    // GET facture par ID
    @GetMapping("/{id}")
    public ResponseEntity<Facture> getById(@PathVariable Long id) {
        return ResponseEntity.ok(factureService.getById(id));
    }

    // GET facture par réservation
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<Facture> getByReservation(@PathVariable Long reservationId) {
        return factureService.getByReservation(reservationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Voir/Télécharger le PDF — accessible sans token grâce au permitAll dans SecurityConfig
    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> getPdf(@PathVariable Long id) throws IOException {
        Facture facture = factureService.getById(id);
        File file = new File(facture.getCheminPdf());

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + facture.getNumero() + ".pdf\"")
                .body(resource);
    }

    // ✅ Envoyer la facture par email
    @PostMapping("/{id}/envoyer")
    public ResponseEntity<String> envoyerParMail(@PathVariable Long id) throws IOException {
        factureService.envoyerFactureParMail(id);
        return ResponseEntity.ok("Facture envoyée par email !");
    }
}