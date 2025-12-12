package com.chess.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Move entity representing a single chess move in a game.
 */
@Entity
@Table(name = "moves", indexes = {
    @Index(name = "idx_game_id", columnList = "gameId"),
    @Index(name = "idx_game_move_number", columnList = "gameId,moveNumber")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Move {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private String fromSquare; // e.g., "e2"

    @Column(nullable = false)
    private String toSquare; // e.g., "e4"

    @Column(nullable = false)
    private String piece; // e.g., "P" (pawn), "N" (knight), etc.

    private String capturedPiece; // Piece captured, if any

    private String promotion; // Promotion piece if pawn reaches end

    @Column(nullable = false)
    private String san; // Standard Algebraic Notation, e.g., "e4", "Nf3"

    @Column(nullable = false)
    private Integer moveNumber; // Sequential move number in game

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}



