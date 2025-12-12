import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChessBoardComponent } from './chess-board.component';
import { PlayerColor } from '../../core/models/game.model';

describe('ChessBoardComponent', () => {
  let component: ChessBoardComponent;
  let fixture: ComponentFixture<ChessBoardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChessBoardComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ChessBoardComponent);
    component = fixture.componentInstance;
    component.playerColor = PlayerColor.WHITE;
    component.isPlayerTurn = true;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should build 8x8 board', () => {
    const board = component.board();
    expect(board.length).toBe(8);
    expect(board[0].length).toBe(8);
  });

  it('should initialize with correct starting position', () => {
    const board = component.board();
    
    // Check white pieces
    const firstRank = board[7]; // Last row for white player
    expect(component.getPieceSymbol(firstRank[0].piece)).toBe('♖'); // Rook
    expect(component.getPieceSymbol(firstRank[4].piece)).toBe('♔'); // King
    
    // Check black pieces
    const eighthRank = board[0]; // First row for white player
    expect(component.getPieceSymbol(eighthRank[0].piece)).toBe('♜'); // Rook
    expect(component.getPieceSymbol(eighthRank[4].piece)).toBe('♚'); // King
  });

  it('should select and deselect square', () => {
    component.onSquareClick('e2');
    expect(component.selectedSquare()).toBe('e2');

    component.onSquareClick('e2');
    expect(component.selectedSquare()).toBeNull();
  });

  it('should emit move event on valid move', (done) => {
    component.moveEmitter.subscribe(move => {
      expect(move.from).toBe('e2');
      expect(move.to).toBe('e4');
      done();
    });

    component.onSquareClick('e2');
    component.onSquareClick('e4');
  });

  it('should not allow move when not player turn', () => {
    component.isPlayerTurn = false;
    fixture.detectChanges();

    component.onSquareClick('e2');
    expect(component.selectedSquare()).toBeNull();
  });
});



