import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { AdminGuard } from './core/guards/admin.guard';

import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';
import { ProfilComponent } from './features/user/profil/profil.component';
import { ChangePasswordComponent } from './features/user/change-password/change-password.component';

import { DashboardComponent } from './features/admin/dashboard/dashboard.component';
import { ReservationsComponent } from './features/admin/reservations/reservations.component';
import { SallesComponent } from './features/admin/salles/salles.component';
import { UsersComponent } from './features/admin/users/users.component';
import { CalendrierComponent } from './features/admin/calendrier/calendrier.component';
import { EntreprisesComponent } from './features/admin/entreprises/entreprises.component';
import { FacturesComponent } from './features/admin/factures/factures.component';
import { PowerbiComponent } from './features/admin/powerbi/powerbi.component';

import { AdminLoginComponent } from './features/admin/admin-login/admin-login.component';
import { AssistantIaComponent } from './features/admin/assistant-ia/assistant-ia.component';

import { SallesClientComponent } from './features/user/salles/salles.component';
import { ReservationComponent } from './features/user/reservation/reservation.component';
import { MesReservationsComponent } from './features/user/mes-reservations/mes-reservations.component';

import { AccueilComponent } from './features/public/accueil/accueil.component';
import { ChatbotComponent } from './features/user/chatbot/chatbot.component';
import { CheckinComponent } from './features/user/checkin/checkin.component';
import { EvaluationComponent } from './features/user/evaluation/evaluation.component';


const routes: Routes = [
  // Auth
  { path: 'login',           component: LoginComponent },
  { path: 'register',        component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password',  component: ResetPasswordComponent },

  // User
  { path: 'profil',          component: ProfilComponent,          canActivate: [AuthGuard] },
  { path: 'change-password', component: ChangePasswordComponent,  canActivate: [AuthGuard] },
  { path: 'salles',          component: SallesClientComponent,    canActivate: [AuthGuard] },
  { path: 'reservations',    component: MesReservationsComponent, canActivate: [AuthGuard] },
  { path: 'reservation/:id', component: ReservationComponent,     canActivate: [AuthGuard] },
  { path: 'chatbot',         component: ChatbotComponent,         canActivate: [AuthGuard] },

  // Admin
  { path: 'startupvillage/admin', component: AdminLoginComponent },  // ✅ nouvelle adresse login admin
  { path: 'admin/dashboard',        component: DashboardComponent,    canActivate: [AdminGuard] },
  { path: 'admin/reservations',     component: ReservationsComponent, canActivate: [AdminGuard] },
  // ✅ Pré-réservations : même composant, statut forcé à EN_ATTENTE, onglets cachés
  { path: 'admin/pre-reservations', component: ReservationsComponent, data: { statutForce: 'EN_ATTENTE' }, canActivate: [AdminGuard] },
  { path: 'admin/salles',           component: SallesComponent,       canActivate: [AdminGuard] },
  { path: 'admin/users',            component: UsersComponent,        canActivate: [AdminGuard] },
  { path: 'admin/calendrier',       component: CalendrierComponent,   canActivate: [AdminGuard] },
  { path: 'admin/entreprises',      component: EntreprisesComponent,  canActivate: [AdminGuard] },
  { path: 'admin/factures',         component: FacturesComponent,     canActivate: [AdminGuard] },
  { path: 'admin/assistant-ia',     component: AssistantIaComponent,  canActivate: [AdminGuard] },
  { path: 'admin/powerbi',          component: PowerbiComponent,      canActivate: [AdminGuard] },


  // Public
  { path: 'checkin/:token', component: CheckinComponent },       // ✅ page de check-in QR (pas de canActivate, accès public)
  { path: 'evaluation/:token', component: EvaluationComponent }, // ✅ NOUVEAU — formulaire d'évaluation depuis le lien email, accès public
  { path: 'accueil', component: AccueilComponent },
  { path: '', redirectTo: 'accueil', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }