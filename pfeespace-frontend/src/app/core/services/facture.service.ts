// ✅ NOUVEAU FICHIER
// Chemin : src/app/core/services/facture.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Facture {
  id: number;
  numero: string;
  montant: number;
  dateGeneration: string;
  statut: string;
  cheminPdf: string;
  reservation: {
    id: number;
    nomComplet: string;
    mail: string;
    date: string;
    heureDebut: string;
    heureFin: string;
    typeUtilisateur: string;
    salle: { nom: string };
  };
}

@Injectable({ providedIn: 'root' })
export class FactureService {

  private api = 'http://localhost:8083/api/factures';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Facture[]> {
    return this.http.get<Facture[]>(this.api);
  }

  getById(id: number): Observable<Facture> {
    return this.http.get<Facture>(`${this.api}/${id}`);
  }

  // ✅ REMPLACER dans facture.service.ts
getPdfUrl(id: number): string {
  const token = localStorage.getItem('token'); // adapte si ta clé est différente
  return `${this.api}/${id}/pdf?token=${token}`;
}

  envoyerParMail(id: number): Observable<string> {
    return this.http.post(`${this.api}/${id}/envoyer`, {},
      { responseType: 'text' });
  }
}
