import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface UserDTO {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  role: string;
  actif: boolean;
  nombreReservations: number;
}

@Injectable({ providedIn: 'root' })
export class UserService {

  private api = 'http://localhost:8083/api/users';

  constructor(private http: HttpClient) {}

  // ✅ Méthodes existantes — inchangées
  getProfil() {
    return this.http.get<any>(`${this.api}/profil`);
  }

  updateProfil(data: any) {
    return this.http.put<any>(`${this.api}/profil`, data);
  }

  changePassword(data: any) {
    return this.http.put(`${this.api}/change-password`, data);
  }

  // ✅ Nouvelles méthodes admin
  getAll() {
    return this.http.get<UserDTO[]>(this.api);
  }

  activer(id: number) {
    return this.http.put(`${this.api}/${id}/activer`, {});
  }

  desactiver(id: number) {
    return this.http.put(`${this.api}/${id}/desactiver`, {});
  }
}