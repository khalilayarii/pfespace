import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { SocialAuthService } from '@abacritt/angularx-social-login';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private api = 'http://localhost:8083/api/auth';

  constructor(
    private http: HttpClient,
    private router: Router,
    private socialAuth: SocialAuthService  // ✅ AJOUT
  ) {}

  register(data: any) {
    return this.http.post(`${this.api}/register`, data);
  }

  login(data: { email: string; password: string }) {
    return this.http.post<{ token: string }>(`${this.api}/login`, {
      email: data.email,
      mdp: data.password
    });
  }

  socialLogin(data: {
    email: string;
    nom: string;
    provider: string;
    providerId: string;
    token: string;
  }) {
    return this.http.post<{ token: string; role: string; nom: string }>(
      `${this.api}/social-login`,
      data
    );
  }

  forgotPassword(email: string) {
    return this.http.post(
      `${this.api}/forgot-password`,
      { email },
      { responseType: 'text' }
    );
  }

  resetPassword(token: string, nouveauMotDePasse: string) {
    return this.http.post(
      `${this.api}/reset-password`,
      { token, nouveauMotDePasse },
      { responseType: 'text' }
    );
  }

  logout() {
    // ✅ Déconnecte la session Google/Facebook avant de vider le localStorage
    this.socialAuth.signOut().catch(() => {});
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('nom');
    this.router.navigate(['/login']);
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  getRole(): string {
    return localStorage.getItem('role') || '';
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }
}