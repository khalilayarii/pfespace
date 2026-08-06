import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface Salle {
  id?: number;
  nom: string;
  description: string;
  capacite: number;
  equipement: string;
  prix: number;
  disponible: boolean;
  image?: string; // ✅ AJOUT
}

@Injectable({ providedIn: 'root' })
export class SalleService {

  private api = 'http://localhost:8083/api/salles';

  constructor(private http: HttpClient) {}

  getAll() {
    return this.http.get<Salle[]>(this.api);
  }

  create(salle: Salle) {
    return this.http.post<Salle>(this.api, salle);
  }

  update(id: number, salle: Salle) {
    return this.http.put<Salle>(`${this.api}/${id}`, salle);
  }

  delete(id: number) {
    return this.http.delete(`${this.api}/${id}`);
  }
}