import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface Reservation {
  id?: number;
  typeUtilisateur: string;
  natureManifestation: string;
  date: string;
  heureDebut: string;
  heureFin: string;
  mail: string;
  nomComplet: string;
  societe: string;
  telephone: string;
  description: string;
  statut?: string;
  salle?: any;
  salleNom?: string;
}

@Injectable({ providedIn: 'root' })
export class ReservationService {

  private api = 'http://localhost:8083/api/reservations';

  constructor(private http: HttpClient) {}

  getAll() {
    return this.http.get<Reservation[]>(this.api);
  }
getByStatut(statut: string) {
  return this.http.get<Reservation[]>(this.api, { params: { statut } });
}

  // ← manquait
  getMesReservations() {
    return this.http.get<Reservation[]>(`${this.api}/mes-reservations`);
  }

  // ← manquait
  creer(data: any) {
    return this.http.post<Reservation>(this.api, data);
  }

  create(salleId: number, reservation: Reservation) {
    return this.http.post<Reservation>(`${this.api}/${salleId}`, reservation);
  }

  confirmer(id: number) {
    return this.http.put(`${this.api}/${id}/confirmer`, {}, { responseType: 'text' });
  }

  refuser(id: number) {
    return this.http.put(`${this.api}/${id}/refuser`, {}, { responseType: 'text' });
  }

  attente(id: number) {
    return this.http.put(`${this.api}/${id}/attente`, {}, { responseType: 'text' });
  }
   // ✅ Méthode manquante — c'est elle qui causait l'erreur
  creerParAdmin(salleId: number, data: any) {
    return this.http.post<any>(`${this.api}/admin/${salleId}`, data);
  }

  supprimer(id: number) {
    return this.http.delete(`${this.api}/${id}`, { responseType: 'text' });
  }

  delete(id: number) {
    return this.http.delete(`${this.api}/${id}`);
  }
  verifierDisponibilite(salleId: number, date: string, 
                      heureDebut: string, heureFin: string) {
  return this.http.get<boolean>(`${this.api}/disponibilite`, {
    params: { salleId: salleId.toString(), date, heureDebut, heureFin }
  });
}
 
}