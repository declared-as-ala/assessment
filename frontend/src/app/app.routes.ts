import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () => import('./auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'lobby',
    loadComponent: () => import('./lobby/lobby.component').then(m => m.LobbyComponent),
    canActivate: [authGuard]
  },
  {
    path: 'game/:id',
    loadComponent: () => import('./game/game.component').then(m => m.GameComponent),
    canActivate: [authGuard]
  },
  {
    path: '',
    redirectTo: '/lobby',
    pathMatch: 'full'
  }
];



