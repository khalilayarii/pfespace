import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private apiUrl = 'http://localhost:8083/api/chatbot';

  constructor(private http: HttpClient) {}

  sendMessage(message: string): Observable<string> {
    const userEmail = localStorage.getItem('userEmail') || '';
    return this.http.post<{ response: string }>(
      `${this.apiUrl}/ask`,
      { message, userEmail }
    ).pipe(map(res => res.response));
  }

  clearHistory(userEmail: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/history`,
      { params: { userEmail } }
    );
  }
}