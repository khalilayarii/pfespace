import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-login',
  templateUrl: './admin-login.component.html',
  styleUrls: ['./admin-login.component.css']
})
export class AdminLoginComponent {
  email = '';
  password = '';
  error = '';
  loading = false;
  showPassword = false;

  constructor(private auth: AuthService, private router: Router) {}

  togglePassword() { this.showPassword = !this.showPassword; }

  onAdminLogin() {
    this.loading = true;
    this.error = '';

    this.auth.login({ email: this.email, password: this.password })
      .subscribe({
        next: (res: any) => {
          if (res.role !== 'ADMIN') {
            this.error = 'Accès refusé : vous n\'êtes pas administrateur.';
            this.loading = false;
            return;
          }
          localStorage.setItem('token', res.token);
          localStorage.setItem('role', res.role);
          localStorage.setItem('nom', res.nom);
          this.loading = false;
          this.router.navigate(['/admin/dashboard']);
        },
        error: (err: any) => {
          this.error = err?.error?.message || 'Email ou mot de passe incorrect';
          this.loading = false;
        }
      });
  }
}