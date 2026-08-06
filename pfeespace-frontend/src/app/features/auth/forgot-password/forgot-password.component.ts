import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {

  email: string = '';
  error: string = '';
  success: string = '';
  loading: boolean = false;

  constructor(private auth: AuthService, private router: Router) {}

  onForgot() {
    this.error = '';
    this.success = '';

    if (!this.email) {
      this.error = 'Veuillez entrer votre adresse email !';
      return;
    }

    this.loading = true;

    this.auth.forgotPassword(this.email).subscribe({
      next: () => {
        this.success = 'Un lien de réinitialisation a été envoyé à votre email !';
        this.loading = false;
      },
      error: () => {
        // Spring envoie du texte → on affiche succès quand même
        this.success = 'Un lien de réinitialisation a été envoyé à votre email !';
        this.loading = false;
      }
    });
  }
}