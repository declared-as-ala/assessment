package com.chess.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Game entity representing a chess match between two players.
 */
@Entity
@Table(name = "games")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long whitePlayerId;

    @Column(nullable = false)
    private Long blackPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Integer moveCount = 0;

    // Current turn: WHITE or BLACK
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerColor currentTurn = PlayerColor.WHITE;

    private Long winnerId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = GameStatus.IN_PROGRESS;
        }
    }

    public enum GameStatus {
        IN_PROGRESS,
        COMPLETED,
        DRAW,
        RESIGNED,
        ABANDONED
    }

    public enum PlayerColor {
        WHITE,
        BLACK
    }
}



