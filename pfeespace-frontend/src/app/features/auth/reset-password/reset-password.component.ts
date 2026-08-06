import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent implements OnInit {

  token: string = '';
  nouveauMotDePasse: string = '';
  confirmMotDePasse: string = '';
  error: string = '';
  success: string = '';
  loading: boolean = false;
  showPassword: boolean = false;
  showConfirm: boolean = false;

  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    // Récupérer le token depuis l'URL
    // Ex: /reset-password?token=abc123
    this.token = this.route.snapshot.queryParams['token'] || '';
  }

  togglePassword() { this.showPassword = !this.showPassword; }
  toggleConfirm()  { this.showConfirm  = !this.showConfirm;  }

  onReset() {
    this.error = '';
    this.success = '';

    if (this.nouveauMotDePasse !== this.confirmMotDePasse) {
      this.error = 'Les mots de passe ne correspondent pas !';
      return;
    }

    if (this.nouveauMotDePasse.length < 6) {
      this.error = 'Le mot de passe doit contenir au moins 6 caractères !';
      return;
    }

    this.loading = true;

    this.auth.resetPassword(this.token, this.nouveauMotDePasse)
      .subscribe({
        next: () => {
          this.success = 'Mot de passe réinitialisé ! Redirection...';
          setTimeout(() => this.router.navigate(['/login']), 2000);
        },
        error: (err: any) => {
          this.error = err.error?.message || 'Token invalide ou expiré !';
          this.loading = false;
        }
      });
  }
}