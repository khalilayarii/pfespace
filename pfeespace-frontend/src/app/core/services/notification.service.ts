import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, interval, Subject } from 'rxjs';
import { startWith, switchMap, takeUntil } from 'rxjs/operators';

export interface NotificationItem {
  id: number;
  titre: string;
  message: string;
  type: string;
  lien?: string;
  lu: boolean;
  dateCreation: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {

  private api = 'http://localhost:8083/api/notifications';
  private stopPolling$ = new Subject<void>();

  constructor(private http: HttpClient) {}

  getNotifications(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(this.api);
  }

  getCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.api}/count`);
  }

  marquerCommeLue(id: number): Observable<void> {
    return this.http.put<void>(`${this.api}/${id}/lire`, {});
  }

  marquerToutesCommeLues(): Observable<void> {
    return this.http.put<void>(`${this.api}/lire-tout`, {});
  }

  // ✅ Démarre le polling toutes les 30s (style Facebook/Gmail)
  startPolling(intervalMs: number = 30000): Observable<{ count: number }> {
    return interval(intervalMs).pipe(
      startWith(0), // ✅ premier appel immédiat, sans attendre 30s
      switchMap(() => this.getCount()),
      takeUntil(this.stopPolling$)
    );
  }

  stopPolling() {
    this.stopPolling$.next();
  }
}