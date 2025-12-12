export interface Game {
  id: number;
  whitePlayerId: number;
  blackPlayerId: number;
  whitePlayerName: string;
  blackPlayerName: string;
  status: GameStatus;
  currentTurn: PlayerColor;
  moveCount: number;
  createdAt: string;
  completedAt?: string;
  winnerId?: number;
  moves?: Move[];
}

export enum GameStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  DRAW = 'DRAW',
  RESIGNED = 'RESIGNED',
  ABANDONED = 'ABANDONED'
}

export enum PlayerColor {
  WHITE = 'WHITE',
  BLACK = 'BLACK'
}

export interface Move {
  id: number;
  gameId: number;
  playerId: number;
  fromSquare: string;
  toSquare: string;
  piece: string;
  capturedPiece?: string;
  promotion?: string;
  san: string;
  moveNumber: number;
  createdAt: string;
}

export interface MoveRequest {
  from: string;
  to: string;
  promotion?: string;
}



