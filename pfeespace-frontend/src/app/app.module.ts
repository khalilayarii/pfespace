import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { JwtInterceptor } from './core/interceptors/jwt.interceptor';

// AUTH
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';

// USER
import { ProfilComponent } from './features/user/profil/profil.component';
import { ChangePasswordComponent } from './features/user/change-password/change-password.component';
import { SallesClientComponent } from './features/user/salles/salles.component';
import { ReservationComponent } from './features/user/reservation/reservation.component';
import { MesReservationsComponent } from './features/user/mes-reservations/mes-reservations.component';
import { CheckinComponent } from './features/user/checkin/checkin.component';

// ADMIN
import { DashboardComponent } from './features/admin/dashboard/dashboard.component';
import { ReservationsComponent } from './features/admin/reservations/reservations.component';
import { SallesComponent } from './features/admin/salles/salles.component';
import { UsersComponent } from './features/admin/users/users.component';
import { CalendrierComponent } from './features/admin/calendrier/calendrier.component';
import { AdminLoginComponent } from './features/admin/admin-login/admin-login.component';
import { EntreprisesComponent } from './features/admin/entreprises/entreprises.component';


// PUBLIC
import { AccueilComponent } from './features/public/accueil/accueil.component';

// SOCIAL LOGIN
import {
  SocialLoginModule,
  SocialAuthServiceConfig,
  GoogleLoginProvider,
  FacebookLoginProvider
} from '@abacritt/angularx-social-login';
import { FacturesComponent } from './features/admin/factures/factures.component';
import { ChatbotComponent } from './features/user/chatbot/chatbot.component';
import { AssistantIaComponent } from './features/admin/assistant-ia/assistant-ia.component';
import { EvaluationComponent } from './features/user/evaluation/evaluation.component';
import { AvisSalleComponent } from './features/user/salles/avis-salle/avis-salle.component';

// CHARTS
import { NgApexchartsModule } from 'ng-apexcharts';
import { PowerbiComponent } from './features/admin/powerbi/powerbi.component';

@NgModule({
  declarations: [
    AppComponent,
    // Auth
    LoginComponent,
    RegisterComponent,
    ForgotPasswordComponent,
    ResetPasswordComponent,
    // User
    ProfilComponent,
    ChangePasswordComponent,
    SallesClientComponent,
    ReservationComponent,
    MesReservationsComponent,
    CheckinComponent,
    AvisSalleComponent,
    // Admin
    DashboardComponent,
    ReservationsComponent,
    SallesComponent,
    UsersComponent,
    CalendrierComponent,
    AdminLoginComponent,
    EntreprisesComponent,
    // Public
    AccueilComponent,
    FacturesComponent,
    ChatbotComponent,
    AssistantIaComponent,
    EvaluationComponent,
    PowerbiComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    SocialLoginModule,
    NgApexchartsModule
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: JwtInterceptor,
      multi: true
    },
    {
      provide: 'SocialAuthServiceConfig',
      useValue: {
        autoLogin: false,
        providers: [
          {
            id: GoogleLoginProvider.PROVIDER_ID,
            provider: new GoogleLoginProvider(
              '715696956822-dqcp1kon86lbu3lhvk0lhcid05l36be1.apps.googleusercontent.com',
              { oneTapEnabled: false }
            )
          },
          {
            id: FacebookLoginProvider.PROVIDER_ID,
            provider: new FacebookLoginProvider('2805383353188191')
          }
        ]
      } as SocialAuthServiceConfig
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }