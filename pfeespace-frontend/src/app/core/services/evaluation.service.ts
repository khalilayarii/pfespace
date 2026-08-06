import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EvaluationRequest {
  noteProprete: number;
  noteEquipement: number;
  noteFaciliteReservation: number;
  capaciteAdaptee: boolean;
  problemesRencontres: string;
  suggestionsAmelioration: string;
}

@Injectable({
  providedIn: 'root'
})
export class EvaluationService {

  private apiUrl = 'http://192.168.0.247:8083/api/evaluation';

  constructor(private http: HttpClient) {}

  soumettreEvaluation(token: string, data: EvaluationRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/${token}`, data);
  }
}