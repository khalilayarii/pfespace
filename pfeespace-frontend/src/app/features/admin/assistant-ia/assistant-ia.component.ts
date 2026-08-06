import {
  Component,
  OnInit,
  ViewChild,
  ElementRef,
  AfterViewChecked
} from '@angular/core';
import { ChatbotService } from '../../../core/services/chatbot.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProfilSharedService } from '../../../core/services/profil-shared.service';

interface Message {
  role: 'user' | 'bot';
  text: string;
}

interface SuggestionRapide {
  label: string;
  icon: string;
  message: string;
}

@Component({
  selector: 'app-assistant-ia',
  templateUrl: './assistant-ia.component.html',
  styleUrls: ['./assistant-ia.component.css']
})
export class AssistantIaComponent implements OnInit, AfterViewChecked {

  profil: any = null;

  messages: Message[] = [
    {
      role: 'bot',
      text: 'Bonjour ! Je suis l\'assistant PfeSpace. Je peux vous aider à <strong>trouver la salle parfaite</strong>, vérifier les disponibilités ou consulter les réservations.'
    }
  ];

  userInput = '';
  loading = false;
  private shouldScroll = false;

  @ViewChild('messagesEnd') messagesEnd!: ElementRef;
  @ViewChild('inputRef') inputRef!: ElementRef;

  suggestionsRapides: SuggestionRapide[] = [
    { label: 'Recommander une salle', icon: '✨', message: 'Recommande-moi une salle' },
    { label: 'Salles disponibles',    icon: '🏢', message: 'Quelles salles sont disponibles ?' },
    { label: 'Réservations en cours', icon: '📅', message: 'Montre-moi les réservations en cours' },
    { label: 'Vérifier un créneau',   icon: '🔍', message: 'Est-ce qu\'une salle est disponible demain de 9h à 11h ?' }
  ];

  constructor(
    private chatbotService: ChatbotService,
    private auth: AuthService,
    private profilShared: ProfilSharedService
  ) {}

  ngOnInit() {
    this.profilShared.loadProfil();
    this.profilShared.profil$.subscribe(p => this.profil = p);
    setTimeout(() => this.inputRef?.nativeElement?.focus(), 200);
  }

  ngAfterViewChecked() {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  sendMessage(texte?: string) {
    const text = (texte || this.userInput).trim();
    if (!text || this.loading) return;

    this.messages.push({ role: 'user', text });
    this.userInput = '';
    this.loading = true;
    this.shouldScroll = true;

    this.chatbotService.sendMessage(text).subscribe({
      next: (response) => {
        this.messages.push({ role: 'bot', text: response });
        this.loading = false;
        this.shouldScroll = true;
      },
      error: () => {
        this.messages.push({ role: 'bot', text: 'Une erreur est survenue. Veuillez réessayer.' });
        this.loading = false;
        this.shouldScroll = true;
      }
    });
  }

  sendSuggestion(s: SuggestionRapide) {
    this.sendMessage(s.message);
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  clearHistory() {
    this.messages = [{ role: 'bot', text: 'Conversation réinitialisée. Comment puis-je vous aider ?' }];
    const email = localStorage.getItem('userEmail') || '';
    if (email) this.chatbotService.clearHistory(email).subscribe();
  }

  formatMessage(text: string): string {
    return text
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/^[•\*\-]\s(.+)/gm, '<li>$1</li>')
      .replace(/(<li>.*<\/li>)/gs, '<ul>$1</ul>')
      .replace(/\n/g, '<br>');
  }

  get showSuggestions(): boolean {
    return this.messages.length <= 1 && !this.loading;
  }

  private scrollToBottom() {
    try {
      this.messagesEnd?.nativeElement?.scrollIntoView({ behavior: 'smooth' });
    } catch (e) {}
  }

  logout() {
    this.auth.logout();
  }
}
