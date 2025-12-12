import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Game, Move, MoveRequest } from '../models/game.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class GameService {
  private readonly API_URL = `${environment.apiUrl}/api/game`;
  
  currentGame = signal<Game | null>(null);

  constructor(private http: HttpClient) {}

  getGame(gameId: number): Observable<Game> {
    return this.http.get<Game>(`${this.API_URL}/${gameId}`);
  }

  getGameMoves(gameId: number, lastMoveId?: number): Observable<Move[]> {
    let params = new HttpParams();
    if (lastMoveId) {
      params = params.set('lastMoveId', lastMoveId.toString());
    }
    return this.http.get<Move[]>(`${this.API_URL}/${gameId}/moves`, { params });
  }

  getActiveGame(): Observable<Game> {
    return this.http.get<Game>(`${this.API_URL}/active`);
  }

  getUserGames(): Observable<Game[]> {
    return this.http.get<Game[]>(`${this.API_URL}/history`);
  }
}



