import { Component, Output, EventEmitter, signal, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Move, MoveRequest, PlayerColor } from '../../core/models/game.model';

interface Square {
  file: string;
  rank: number;
  piece: string | null;
  color: 'light' | 'dark';
  coordinate: string;
}

@Component({
  selector: 'app-chess-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chess-board.component.html',
  styleUrls: ['./chess-board.component.css']
})
export class ChessBoardComponent {
  moves = input<Move[]>([]);
  playerColor = input<PlayerColor>(PlayerColor.WHITE);
  isPlayerTurn = input<boolean>(false);
  @Output() moveEmitter = new EventEmitter<MoveRequest>();

  selectedSquare = signal<string | null>(null);
  validMoves = signal<string[]>([]);
  board = computed(() => this.buildBoard());

  private readonly files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
  private readonly initialPosition = 
    'rnbqkbnr' +
    'pppppppp' +
    '        ' +
    '        ' +
    '        ' +
    '        ' +
    'PPPPPPPP' +
    'RNBQKBNR';

  private readonly pieceSymbols: { [key: string]: string } = {
    'K': '♔', 'Q': '♕', 'R': '♖', 'B': '♗', 'N': '♘', 'P': '♙',
    'k': '♚', 'q': '♛', 'r': '♜', 'b': '♝', 'n': '♞', 'p': '♟'
  };

  private buildBoard(): Square[][] {
    const position = this.calculatePosition();
    const board: Square[][] = [];

    for (let rank = 8; rank >= 1; rank--) {
      const row: Square[] = [];
      for (let fileIdx = 0; fileIdx < 8; fileIdx++) {
        const file = this.files[fileIdx];
        const coordinate = `${file}${rank}`;
        const piece = position[coordinate] || null;
        const color = (fileIdx + rank) % 2 === 0 ? 'dark' : 'light';
        
        row.push({ file, rank, piece, color, coordinate });
      }
      board.push(row);
    }

    // Flip board if playing as black
    if (this.playerColor() === PlayerColor.BLACK) {
      return board.reverse().map(row => row.reverse());
    }

    return board;
  }

  private calculatePosition(): { [square: string]: string } {
    const position: { [square: string]: string } = {};

    // Set initial position
    for (let rank = 0; rank < 8; rank++) {
      for (let file = 0; file < 8; file++) {
        const coordinate = this.files[file] + (rank + 1);
        const index = (7 - rank) * 8 + file;
        const piece = this.initialPosition[index];
        if (piece !== ' ') {
          position[coordinate] = piece;
        }
      }
    }

    // Apply moves
    for (const move of this.moves()) {
      delete position[move.fromSquare];
      let piece = move.piece;
      
      // Handle promotion
      if (move.promotion) {
        piece = move.promotion;
      }
      
      // Adjust case based on move number (odd = white, even = black)
      const isWhiteMove = move.moveNumber % 2 === 1;
      position[move.toSquare] = isWhiteMove ? piece.toUpperCase() : piece.toLowerCase();
    }

    return position;
  }

  getPieceSymbol(piece: string | null): string {
    return piece ? this.pieceSymbols[piece] || '' : '';
  }

  isSquareSelected(coordinate: string): boolean {
    return this.selectedSquare() === coordinate;
  }

  onSquareClick(coordinate: string): void {
    if (!this.isPlayerTurn()) return;

    const selected = this.selectedSquare();

    if (!selected) {
      // First click - select piece
      const position = this.calculatePosition();
      const piece = position[coordinate];
      
      if (piece) {
        const isWhitePiece = piece === piece.toUpperCase();
        const isPlayerPiece = (this.playerColor() === PlayerColor.WHITE && isWhitePiece) ||
                             (this.playerColor() === PlayerColor.BLACK && !isWhitePiece);
        
        if (isPlayerPiece) {
          this.selectedSquare.set(coordinate);
          this.validMoves.set(this.calculateValidMoves(coordinate, piece, position));
        }
      }
    } else {
      // Second click - attempt move
      if (selected === coordinate) {
        // Deselect
        this.selectedSquare.set(null);
        this.validMoves.set([]);
      } else {
        // Make move
        this.makeMove(selected, coordinate);
        this.selectedSquare.set(null);
        this.validMoves.set([]);
      }
    }
  }

  private calculateValidMoves(from: string, piece: string, position: { [square: string]: string }): string[] {
    const moves: string[] = [];
    const pieceType = piece.toUpperCase();
    const fromFile = from.charCodeAt(0) - 97; // 'a' = 0
    const fromRank = parseInt(from[1]) - 1;
    const isPieceWhite = piece === piece.toUpperCase();
    
    if (pieceType === 'P') {
      // Special handling for pawns
      return this.calculatePawnMoves(fromFile, fromRank, isPieceWhite, position);
    }
    
    // Generate possible moves based on piece type
    const directions = this.getPieceDirections(pieceType, isPieceWhite);
    
    for (const [fileDir, rankDir, maxDistance] of directions) {
      for (let dist = 1; dist <= maxDistance; dist++) {
        const toFile = fromFile + fileDir * dist;
        const toRank = fromRank + rankDir * dist;
        
        if (toFile < 0 || toFile > 7 || toRank < 0 || toRank > 7) break;
        
        const to = String.fromCharCode(97 + toFile) + (toRank + 1);
        const targetPiece = position[to];
        
        // Empty square
        if (!targetPiece || targetPiece === ' ') {
          moves.push(to);
        } else {
          // Piece exists
          const isTargetWhite = targetPiece === targetPiece.toUpperCase();
          if (isTargetWhite !== isPieceWhite) {
            moves.push(to); // Can capture
          }
          break; // Blocked by any piece
        }
        
        if (pieceType === 'N' || pieceType === 'K') break; // Knights and kings don't slide
      }
    }
    
    return moves;
  }

  private calculatePawnMoves(fromFile: number, fromRank: number, isWhite: boolean, position: { [square: string]: string }): string[] {
    const moves: string[] = [];
    const direction = isWhite ? 1 : -1;
    const startRank = isWhite ? 1 : 6;
    
    // Forward one square
    const oneSquare = String.fromCharCode(97 + fromFile) + (fromRank + 1 + direction);
    const pieceAtOne = position[oneSquare];
    if ((!pieceAtOne || pieceAtOne === ' ') && fromRank + direction >= 0 && fromRank + direction <= 7) {
      moves.push(oneSquare);
      
      // Forward two squares from starting position
      if (fromRank === startRank) {
        const twoSquare = String.fromCharCode(97 + fromFile) + (fromRank + 1 + direction * 2);
        const pieceAtTwo = position[twoSquare];
        if (!pieceAtTwo || pieceAtTwo === ' ') {
          moves.push(twoSquare);
        }
      }
    }
    
    // Diagonal captures
    for (const fileDelta of [-1, 1]) {
      const captureFile = fromFile + fileDelta;
      const captureRank = fromRank + direction;
      
      if (captureFile >= 0 && captureFile <= 7 && captureRank >= 0 && captureRank <= 7) {
        const captureSquare = String.fromCharCode(97 + captureFile) + (captureRank + 1);
        const targetPiece = position[captureSquare];
        
        if (targetPiece && targetPiece !== ' ') {
          const isTargetWhite = targetPiece === targetPiece.toUpperCase();
          if (isTargetWhite !== isWhite) {
            moves.push(captureSquare); // Can capture diagonally
          }
        }
      }
    }
    
    return moves;
  }

  private getPieceDirections(pieceType: string, isWhite: boolean): [number, number, number][] {
    switch (pieceType) {
      case 'N': // Knight
        return [[2, 1, 1], [2, -1, 1], [-2, 1, 1], [-2, -1, 1],
                [1, 2, 1], [1, -2, 1], [-1, 2, 1], [-1, -2, 1]];
      case 'B': // Bishop
        return [[1, 1, 8], [1, -1, 8], [-1, 1, 8], [-1, -1, 8]];
      case 'R': // Rook
        return [[1, 0, 8], [-1, 0, 8], [0, 1, 8], [0, -1, 8]];
      case 'Q': // Queen
        return [[1, 0, 8], [-1, 0, 8], [0, 1, 8], [0, -1, 8],
                [1, 1, 8], [1, -1, 8], [-1, 1, 8], [-1, -1, 8]];
      case 'K': // King
        return [[1, 0, 1], [-1, 0, 1], [0, 1, 1], [0, -1, 1],
                [1, 1, 1], [1, -1, 1], [-1, 1, 1], [-1, -1, 1]];
      default:
        return [];
    }
  }

  isValidMoveSquare(coordinate: string): boolean {
    return this.validMoves().includes(coordinate);
  }

  private makeMove(from: string, to: string): void {
    const moveRequest: MoveRequest = { from, to };
    
    // Check for pawn promotion
    const position = this.calculatePosition();
    const piece = position[from];
    if (piece && piece.toUpperCase() === 'P') {
      const toRank = parseInt(to[1]);
      if (toRank === 8 || toRank === 1) {
        moveRequest.promotion = 'Q'; // Auto-promote to queen for simplicity
      }
    }

    this.moveEmitter.emit(moveRequest);
  }
}


