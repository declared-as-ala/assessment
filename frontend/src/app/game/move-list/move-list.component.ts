import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Move } from '../../core/models/game.model';

interface MovePair {
  moveNumber: number;
  white: Move | null;
  black: Move | null;
}

@Component({
  selector: 'app-move-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './move-list.component.html',
  styleUrls: ['./move-list.component.css']
})
export class MoveListComponent {
  @Input() moves: Move[] = [];

  movePairs = computed(() => {
    const pairs: MovePair[] = [];
    const movesCopy = [...this.moves];

    for (let i = 0; i < movesCopy.length; i += 2) {
      const white = movesCopy[i];
      const black = movesCopy[i + 1] || null;
      
      pairs.push({
        moveNumber: Math.floor(i / 2) + 1,
        white,
        black
      });
    }

    return pairs;
  });
}



