import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CheckinResponse {
  id: number;
  statut: string;
  checkInTime: string;
  salle: { nom: string };
  date: string;
  heureDebut: string;
  heureFin: string;
}

@Injectable({
  providedIn: 'root'
})
export class CheckinService {
 private apiUrl = 'http://192.168.0.247:8083/api/checkin';

  constructor(private http: HttpClient) {}

  checkin(token: string): Observable<CheckinResponse> {
    return this.http.post<CheckinResponse>(`${this.apiUrl}/${token}`, {});
  }
}