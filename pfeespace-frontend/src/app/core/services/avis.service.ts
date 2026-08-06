import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AvisService {
  private apiUrl = 'http://192.168.0.247:8083/api/salles';

  constructor(private http: HttpClient) {}

  getAvisSalle(salleId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${salleId}/avis`);
  }
}