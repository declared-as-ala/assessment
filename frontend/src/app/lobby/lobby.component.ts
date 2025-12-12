import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../core/services/auth.service';
import { WebSocketService } from '../core/services/websocket.service';
import { User } from '../core/models/user.model';
import { Invitation } from '../core/models/invitation.model';
import { Game } from '../core/models/game.model';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-lobby',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './lobby.component.html',
  styleUrls: ['./lobby.component.css']
})
export class LobbyComponent implements OnInit, OnDestroy {
  currentUser = this.authService.currentUser;
  onlinePlayers = signal<User[]>([]);
  pendingInvitation = signal<Invitation | null>(null);
  pendingInvitations = signal<Invitation[]>([]);
  loading = signal(true);

  constructor(
    private authService: AuthService,
    private wsService: WebSocketService,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeWebSocket();
    this.loadOnlinePlayers();
    this.checkActiveGame();
  }

  ngOnDestroy(): void {
    this.wsService.unsubscribe('/topic/presence');
    this.wsService.unsubscribe('/user/queue/invitations');
    this.wsService.unsubscribe('/user/queue/game-start');
  }

  private async initializeWebSocket(): Promise<void> {
    try {
      console.log('Connecting to WebSocket...');
      await this.wsService.connect();
      console.log('WebSocket connected successfully!');
      
      // Subscribe to presence updates
      console.log('Subscribing to /topic/presence');
      this.wsService.subscribe<User[]>('/topic/presence', (users) => {
        console.log('Presence update received:', users);
        this.onlinePlayers.set(users.filter(u => u.id !== this.currentUser()?.id));
      });

      // Subscribe to invitations
      console.log('Subscribing to /user/queue/invitations');
      this.wsService.subscribe<Invitation>('/user/queue/invitations', (invitation) => {
        console.log('Received invitation:', invitation);
        this.pendingInvitation.set(invitation);
        this.pendingInvitations.update(list => {
          if (list.find(i => i.id === invitation.id)) return list;
          return [...list, invitation];
        });
      });

      // Subscribe to game start
      console.log('Subscribing to /user/queue/game-start');
      this.wsService.subscribe<Game>('/user/queue/game-start', (game) => {
        console.log('Game started:', game);
        this.router.navigate(['/game', game.id]);
      });

      // Notify presence
      console.log('Sending presence notification');
      this.wsService.send('/app/lobby/presence');
    } catch (error) {
      console.error('WebSocket connection failed', error);
      alert('WebSocket connection failed. Please refresh the page.');
    }
  }

  private loadOnlinePlayers(): void {
    this.http.get<User[]>(`${environment.apiUrl}/api/lobby/players`).subscribe({
      next: (users) => {
        this.onlinePlayers.set(users.filter(u => u.id !== this.currentUser()?.id));
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load players', err);
        this.loading.set(false);
      }
    });
  }

  private checkActiveGame(): void {
    this.http.get<Game>(`${environment.apiUrl}/api/game/active`).subscribe({
      next: (game) => {
        if (game && game.id) {
          this.router.navigate(['/game', game.id]);
        }
      },
      error: () => {
        // No active game, stay in lobby
      }
    });
  }

  showFirstInvitation(): void {
    const invitations = this.pendingInvitations();
    if (invitations.length > 0) {
      this.pendingInvitation.set(invitations[0]);
    }
  }

  invitePlayer(player: User): void {
    console.log('Sending invitation to:', player);
    this.wsService.send('/app/invite', { toUserId: player.id });
    alert(`Invitation sent to ${player.displayName}!`);
  }

  acceptInvitation(): void {
    const invitation = this.pendingInvitation();
    if (invitation) {
      console.log('Accepting invitation:', invitation.id);
      this.wsService.send('/app/invitation/accept', { invitationId: invitation.id });
      this.pendingInvitation.set(null);
      this.pendingInvitations.set(this.pendingInvitations().filter(i => i.id !== invitation.id));
      
      // Show loading state
      alert('Creating game... Please wait.');
    }
  }

  declineInvitation(): void {
    const invitation = this.pendingInvitation();
    if (invitation) {
      this.wsService.send('/app/invitation/decline', { invitationId: invitation.id });
      this.pendingInvitation.set(null);
      this.pendingInvitations.set(this.pendingInvitations().filter(i => i.id !== invitation.id));
    }
  }

  logout(): void {
    this.wsService.disconnect();
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}


