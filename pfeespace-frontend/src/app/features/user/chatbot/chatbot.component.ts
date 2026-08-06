import {
  Component,
  ViewChild,
  ElementRef,
  AfterViewChecked
} from '@angular/core';
import { ChatbotService } from '../../../core/services/chatbot.service';

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
  selector: 'app-chatbot',
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.css']
})
export class ChatbotComponent implements AfterViewChecked {

  messages: Message[] = [
    {
      role: 'bot',
      text: 'Bonjour ! Je suis l\'assistant PfeSpace. Je peux vous aider à <strong>trouver la salle parfaite</strong>, vérifier les disponibilités ou consulter vos réservations.'
    }
  ];

  userInput = '';
  loading = false;
  isOpen = false;
  unreadCount = 0;
  private shouldScroll = false;

  @ViewChild('messagesEnd') messagesEnd!: ElementRef;
  @ViewChild('inputRef') inputRef!: ElementRef;

  suggestionsRapides: SuggestionRapide[] = [
    { label: 'Recommander une salle', icon: '✨', message: 'Recommande-moi une salle' },
    { label: 'Salles disponibles',    icon: '🏢', message: 'Quelles salles sont disponibles ?' },
    { label: 'Mes réservations',      icon: '📅', message: 'Montre-moi mes réservations' },
    { label: 'Vérifier un créneau',   icon: '🔍', message: 'Est-ce qu\'une salle est disponible demain de 9h à 11h ?' }
  ];

  constructor(private chatbotService: ChatbotService) {}

  ngAfterViewChecked() {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  toggleChat() {
    this.isOpen = !this.isOpen;
    console.log('Chat ouvert ?', this.isOpen);
    if (this.isOpen) {
      this.unreadCount = 0;
      setTimeout(() => this.inputRef?.nativeElement?.focus(), 200);
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
        if (!this.isOpen) this.unreadCount++;
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
}
