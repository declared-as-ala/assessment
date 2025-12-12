import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { WebSocketService } from '../core/services/websocket.service';
import { GameService } from '../core/services/game.service';
import { Game, GameStatus, Move, MoveRequest, PlayerColor } from '../core/models/game.model';
import { ChessBoardComponent } from './chess-board/chess-board.component';
import { MoveListComponent } from './move-list/move-list.component';

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [CommonModule, ChessBoardComponent, MoveListComponent],
  templateUrl: './game.component.html',
  styleUrls: ['./game.component.css']
})
export class GameComponent implements OnInit, OnDestroy {
  gameId = signal<number>(0);
  game = signal<Game | null>(null);
  moves = signal<Move[]>([]);
  currentUser = this.authService.currentUser;
  gameEndMessage = signal<string | null>(null);
  showGameEndModal = signal<boolean>(false);
  
  playerColor = computed(() => {
    const g = this.game();
    const user = this.currentUser();
    if (!g || !user) return null;
    return g.whitePlayerId === user.id ? PlayerColor.WHITE : PlayerColor.BLACK;
  });

  isPlayerTurn = computed(() => {
    const g = this.game();
    const color = this.playerColor();
    return !!(g && color && g.currentTurn === color);
  });

  opponentName = computed(() => {
    const g = this.game();
    const color = this.playerColor();
    if (!g || !color) return '';
    return color === PlayerColor.WHITE ? g.blackPlayerName : g.whitePlayerName;
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private wsService: WebSocketService,
    private gameService: GameService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.gameId.set(+id);
      this.loadGame();
      this.initializeWebSocket();
    }
  }

  ngOnDestroy(): void {
    const id = this.gameId();
    if (id) {
      this.wsService.unsubscribe(`/topic/game/${id}/moves`);
      this.wsService.unsubscribe(`/topic/game/${id}/resigned`);
    }
  }

  private loadGame(): void {
    console.log('Loading game:', this.gameId());
    this.gameService.getGame(this.gameId()).subscribe({
      next: (game) => {
        console.log('Game loaded:', game);
        this.game.set(game);
        this.moves.set(game.moves || []);
        console.log('Initial moves loaded:', game.moves?.length || 0);

        if (game.status !== GameStatus.IN_PROGRESS) {
          this.handleGameFinished(game);
        }
      },
      error: (err) => {
        console.error('Failed to load game', err);
        this.router.navigate(['/lobby']);
      }
    });
  }

  private fetchMissedMoves(): void {
    const currentMoves = this.moves();
    const lastMoveId = currentMoves.length > 0 ? currentMoves[currentMoves.length - 1].id : 0;
    
    console.log('Fetching moves since ID:', lastMoveId);
    
    this.gameService.getGameMoves(this.gameId(), lastMoveId).subscribe({
      next: (newMoves) => {
        if (newMoves.length > 0) {
          console.log('Fetched', newMoves.length, 'missed moves');
          this.moves.update(moves => [...moves, ...newMoves]);
          
          // Update game turn based on last move
          const lastMove = newMoves[newMoves.length - 1];
          const isLastMoveWhite = lastMove.moveNumber % 2 === 1;
          this.game.update(g => {
            if (!g) return g;
            return {
              ...g,
              moveCount: lastMove.moveNumber,
              currentTurn: isLastMoveWhite ? PlayerColor.BLACK : PlayerColor.WHITE
            };
          });
        }
      },
      error: (err) => console.error('Failed to fetch missed moves', err)
    });
  }

  private async initializeWebSocket(): Promise<void> {
    try {
      console.log('Game component: Connecting to WebSocket...');
      await this.wsService.connect();
      console.log('Game component: WebSocket connected');

      // Subscribe to move updates immediately after connecting to avoid missing events
      const moveTopic = `/topic/game/${this.gameId()}/moves`;
      console.log('Subscribing to:', moveTopic);
      this.wsService.subscribe<Move>(moveTopic, (move) => {
        console.log('🎯 MOVE RECEIVED via WebSocket:', move);
        
        // Add move to list
        this.moves.update(moves => {
          // Check if move already exists (avoid duplicates)
          if (moves.find(m => m.id === move.id)) {
            console.log('⚠️ Duplicate move ignored:', move.id);
            return moves;
          }
          console.log('➕ Adding new move to list');
          return [...moves, move];
        });
        
        // Update game state
        this.game.update(g => {
          if (!g) return g;
          console.log('🔄 Updating game state. Current turn was:', g.currentTurn);
          const newTurn = g.currentTurn === PlayerColor.WHITE ? PlayerColor.BLACK : PlayerColor.WHITE;
          console.log('🔄 New turn:', newTurn);
          return {
            ...g,
            moveCount: g.moveCount + 1,
            currentTurn: newTurn
          };
        });
        
        console.log('✅ Move applied successfully. Total moves:', this.moves().length);
      });

      // Subscribe to resignation events (winners need this instantly)
      const resignTopic = `/topic/game/${this.gameId()}/resigned`;
      console.log('Subscribing to:', resignTopic);
      
      this.wsService.subscribe<Game>(resignTopic, (updatedGame) => {
        console.log('🚨 RESIGNATION EVENT RECEIVED:', updatedGame);
        this.handleGameFinished(updatedGame);
      });

      // Join game room
      console.log('Joining game room...');
      this.wsService.send(`/app/game/${this.gameId()}/join`);
      console.log('✅ Game room joined');

      // Fetch any missed moves after subscriptions are in place
      this.fetchMissedMoves();
    } catch (error) {
      console.error('❌ WebSocket connection failed', error);
    }
  }

  onMove(moveRequest: MoveRequest): void {
    if (this.game()?.status !== GameStatus.IN_PROGRESS) {
      console.warn('Ignoring move: game already finished');
      return;
    }

    console.log('Sending move:', moveRequest);
    console.log('To destination:', `/app/game/${this.gameId()}/move`);
    this.wsService.send(`/app/game/${this.gameId()}/move`, moveRequest);
  }

  resignGame(): void {
    if (this.game()?.status !== GameStatus.IN_PROGRESS) {
      console.warn('Ignoring resign: game already finished');
      return;
    }

    if (confirm('Are you sure you want to resign? Your opponent will win!')) {
      console.log('🏳️ Resigning from game:', this.gameId());
      this.wsService.send(`/app/game/${this.gameId()}/resign`, {});
      console.log('Resignation request sent, waiting for server confirmation...');

      // Optimistically show result so the resigning player sees it immediately
      const currentGame = this.game();
      const userId = this.currentUser()?.id;
      if (currentGame && userId) {
        const winnerId = currentGame.whitePlayerId === userId
          ? currentGame.blackPlayerId
          : currentGame.whitePlayerId;

        this.handleGameFinished({
          ...currentGame,
          status: GameStatus.COMPLETED,
          winnerId
        });
      }
    }
  }

  closeGameEndModal(): void {
    this.showGameEndModal.set(false);
    this.gameEndMessage.set(null);
    console.log('Returning to lobby...');
    this.backToLobby();
  }

  backToLobby(): void {
    this.router.navigate(['/lobby']);
  }

  private handleGameFinished(finishedGame: Game): void {
    if (this.showGameEndModal()) {
      return; // Already handled, avoid duplicate modals
    }

    this.game.set(finishedGame);

    const isWinner = finishedGame.winnerId === this.currentUser()?.id;
    this.gameEndMessage.set(isWinner ? 'You won!' : 'You lost!');
    this.showGameEndModal.set(true);
  }
}

