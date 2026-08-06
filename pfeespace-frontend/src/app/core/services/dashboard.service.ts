import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DashboardKpis {
  totalReservations: number;
  totalSalles: number;
  totalEntreprises: number;
  dureeMoyenneHeures: number;
  reservantsActifs: number;
  tauxOccupation: number;
}

export interface MoisData {
  mois: string;
  total: number;
}

export interface StatutData {
  statut: string;
  total: number;
}

export interface SalleData {
  salle: string;
  total: number;
}

export interface AnneeData {
  annee: number;
  total: number;
}

export interface CreneauData {
  creneau: string;
  total: number;
}

export interface GratuitPayantData {
  nbGratuit: number;
  nbPayant: number;
  pctGratuit: number;
  pctPayant: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {

  private baseUrl = 'http://localhost:8083/api/dashboard';

  constructor(private http: HttpClient) {}

  getKpis(): Observable<DashboardKpis> {
    return this.http.get<DashboardKpis>(`${this.baseUrl}/kpis`);
  }

  getReservationsParMois(annee?: number): Observable<MoisData[]> {
    let params = new HttpParams();
    if (annee) {
      params = params.set('annee', annee.toString());
    }
    return this.http.get<MoisData[]>(`${this.baseUrl}/reservations-par-mois`, { params });
  }

  getReservationsParStatut(): Observable<StatutData[]> {
    return this.http.get<StatutData[]>(`${this.baseUrl}/reservations-par-statut`);
  }

  getReservationsParSalle(): Observable<SalleData[]> {
    return this.http.get<SalleData[]>(`${this.baseUrl}/reservations-par-salle`);
  }

  getReservationsParAnnee(): Observable<AnneeData[]> {
    return this.http.get<AnneeData[]>(`${this.baseUrl}/reservations-par-annee`);
  }

  getCreneauxHoraires(): Observable<CreneauData[]> {
    return this.http.get<CreneauData[]>(`${this.baseUrl}/creneaux-horaires`);
  }

  getGratuitVsPayant(): Observable<GratuitPayantData> {
    return this.http.get<GratuitPayantData>(`${this.baseUrl}/gratuit-vs-payant`);
  }
}