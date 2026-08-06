import { Component, OnInit } from '@angular/core';
import { UserService, UserDTO } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css']
})
export class UsersComponent implements OnInit {

  users: UserDTO[] = [];
  loading = false;
  message = '';
  messageType = '';
  sidebarOpen = false;

  constructor(
    private userService: UserService,
    private auth: AuthService
  ) {}

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.loading = true;
    this.userService.getAll().subscribe({
      next: (data) => {
        this.users = data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  activer(id: number) {
    this.userService.activer(id).subscribe({
      next: () => {
        this.showMessage('✅ Utilisateur activé avec succès !', 'success');
        this.loadUsers();
      },
      error: () => this.showMessage('Erreur lors de l\'activation', 'error')
    });
  }

  desactiver(id: number) {
    this.userService.desactiver(id).subscribe({
      next: () => {
        this.showMessage('🔴 Utilisateur désactivé !', 'success');
        this.loadUsers();
      },
      error: () => this.showMessage('Erreur lors de la désactivation', 'error')
    });
  }

  showMessage(msg: string, type: string) {
    this.message = msg;
    this.messageType = type;
    setTimeout(() => this.message = '', 3000);
  }

  logout() { this.auth.logout(); }
}