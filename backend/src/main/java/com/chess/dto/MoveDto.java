package com.chess.dto;

import com.chess.model.Move;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveDto {
    private Long id;
    private Long gameId;
    private Long playerId;
    private String fromSquare;
    private String toSquare;
    private String piece;
    private String capturedPiece;
    private String promotion;
    private String san;
    private Integer moveNumber;
    private LocalDateTime createdAt;
    
    public static MoveDto fromEntity(Move move) {
        return MoveDto.builder()
                .id(move.getId())
                .gameId(move.getGameId())
                .playerId(move.getPlayerId())
                .fromSquare(move.getFromSquare())
                .toSquare(move.getToSquare())
                .piece(move.getPiece())
                .capturedPiece(move.getCapturedPiece())
                .promotion(move.getPromotion())
                .san(move.getSan())
                .moveNumber(move.getMoveNumber())
                .createdAt(move.getCreatedAt())
                .build();
    }
}



