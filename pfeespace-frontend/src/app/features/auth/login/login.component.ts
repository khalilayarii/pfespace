import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { SocialAuthService, FacebookLoginProvider, GoogleLoginProvider } from '@abacritt/angularx-social-login';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  email = '';
  password = '';
  error = '';
  loading = false;
  showPassword = false;

  constructor(
    private auth: AuthService,
    private router: Router,
    private socialAuth: SocialAuthService
  ) {}

  private redirectByRole(role: string) {
    if (role === 'ADMIN') {
      this.router.navigate(['/admin/dashboard']);
    } else {
      this.router.navigate(['/salles']);
    }
  }

  ngOnInit() {
    this.socialAuth.authState.subscribe({
      next: (user) => {
        if (user && localStorage.getItem('token') === null) {
          this.loading = true;
          this.error = '';

          this.auth.socialLogin({
            email: user.email,
            nom: user.firstName + ' ' + user.lastName,
            provider: user.provider,
            providerId: user.id,
            token: user.idToken ?? user.authToken
          }).subscribe({
            next: (res: any) => {
              localStorage.setItem('token', res.token);
              localStorage.setItem('role', res.role);
              localStorage.setItem('nom', res.nom);
              this.loading = false;
              this.redirectByRole(res.role);
            },
            error: (err: any) => {
              console.error('Erreur social login backend:', err);
              this.error = err?.error?.message || 'Erreur lors de la connexion sociale';
              this.loading = false;
            }
          });
        }
      },
      error: (err: any) => {
        console.error('Erreur authState:', err);
        this.error = 'Erreur lors de la connexion sociale';
        this.loading = false;
      }
    });
  }

  togglePassword() { this.showPassword = !this.showPassword; }
  hideImg(event: any) { event.target.style.display = 'none'; }

  loginWithGoogle() {
    this.loading = true;
    this.error = '';

    this.socialAuth.signIn(GoogleLoginProvider.PROVIDER_ID)
      .then((user: any) => {
        this.auth.socialLogin({
          email: user.email,
          nom: user.firstName + ' ' + user.lastName,
          provider: user.provider,
          providerId: user.id,
          token: user.idToken ?? user.authToken
        }).subscribe({
          next: (res: any) => {
            localStorage.setItem('token', res.token);
            localStorage.setItem('role', res.role);
            localStorage.setItem('nom', res.nom);
            this.loading = false;
            this.redirectByRole(res.role);
          },
          error: (err: any) => {
            console.error('Erreur Google backend:', err);
            this.error = err?.error?.message || 'Erreur connexion Google';
            this.loading = false;
          }
        });
      })
      .catch((err: any) => {
        console.error('Erreur Google popup:', err);
        if (err?.error !== 'popup_closed_by_user') {
          this.error = 'Erreur lors de l\'ouverture de Google';
        }
        this.loading = false;
      });
  }

  loginWithFacebook() {
    this.loading = true;
    this.error = '';

    this.socialAuth.signIn(FacebookLoginProvider.PROVIDER_ID)
      .then((user: any) => {
        this.auth.socialLogin({
          email: user.email,
          nom: user.firstName + ' ' + user.lastName,
          provider: user.provider,
          providerId: user.id,
          token: user.authToken
        }).subscribe({
          next: (res: any) => {
            localStorage.setItem('token', res.token);
            localStorage.setItem('role', res.role);
            localStorage.setItem('nom', res.nom);
            this.loading = false;
            this.redirectByRole(res.role);
          },
          error: (err: any) => {
            console.error('Erreur Facebook backend:', err);
            this.error = err?.error?.message || 'Erreur connexion Facebook';
            this.loading = false;
          }
        });
      })
      .catch((err: any) => {
        console.error('Erreur Facebook popup:', err);
        if (err?.error !== 'popup_closed_by_user') {
          this.error = 'Erreur lors de l\'ouverture de Facebook';
        }
        this.loading = false;
      });
  }

  onLogin() {
    this.loading = true;
    this.error = '';

    this.auth.login({ email: this.email, password: this.password })
      .subscribe({
        next: (res: any) => {
          localStorage.setItem('token', res.token);
          localStorage.setItem('role', res.role);
          localStorage.setItem('nom', res.nom);
          this.loading = false;
          this.redirectByRole(res.role);
        },
        error: (err: any) => {
          console.error('Erreur login:', err);
          this.error = err?.error?.message || 'Email ou mot de passe incorrect';
          this.loading = false;
        }
      });
  }
}