package com.chess.dto;

import com.chess.model.Game;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameDto {
    private Long id;
    private Long whitePlayerId;
    private Long blackPlayerId;
    private String whitePlayerName;
    private String blackPlayerName;
    private String status;
    private String currentTurn;
    private Integer moveCount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Long winnerId;
    private List<MoveDto> moves;
    
    public static GameDto fromEntity(Game game) {
        return GameDto.builder()
                .id(game.getId())
                .whitePlayerId(game.getWhitePlayerId())
                .blackPlayerId(game.getBlackPlayerId())
                .status(game.getStatus().name())
                .currentTurn(game.getCurrentTurn().name())
                .moveCount(game.getMoveCount())
                .createdAt(game.getCreatedAt())
                .completedAt(game.getCompletedAt())
                .winnerId(game.getWinnerId())
                .build();
    }
}



