import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Entreprise {
  id?: number;
  nom: string;
  numFiscal: string;
  adresse: string;
  email: string;
  telephone: string;
  estMembre: boolean;
}

@Injectable({ providedIn: 'root' })
export class EntrepriseService {

  private apiUrl = 'http://localhost:8083/api/entreprises';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Entreprise[]> {
    return this.http.get<Entreprise[]>(this.apiUrl);
  }

  create(entreprise: Entreprise): Observable<Entreprise> {
    return this.http.post<Entreprise>(this.apiUrl, entreprise);
  }

  update(id: number, entreprise: Entreprise): Observable<Entreprise> {
    return this.http.put<Entreprise>(`${this.apiUrl}/${id}`, entreprise);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  verifierNumFiscal(numFiscal: string): Observable<Entreprise> {
    return this.http.get<Entreprise>(`${this.apiUrl}/verifier/${numFiscal}`);
  }
}