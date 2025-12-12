package com.chess.controller;

import com.chess.dto.GameDto;
import com.chess.dto.MoveDto;
import com.chess.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for game operations.
 */
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/{gameId}")
    public ResponseEntity<GameDto> getGame(@PathVariable Long gameId) {
        return ResponseEntity.ok(gameService.getGame(gameId));
    }

    @GetMapping("/{gameId}/moves")
    public ResponseEntity<List<MoveDto>> getGameMoves(
            @PathVariable Long gameId,
            @RequestParam(required = false) Long lastMoveId) {
        return ResponseEntity.ok(gameService.getGameMoves(gameId, lastMoveId));
    }

    @GetMapping("/active")
    public ResponseEntity<GameDto> getActiveGame(Authentication authentication) {
        Long userId = extractUserId(authentication);
        GameDto game = gameService.getActiveGame(userId);
        if (game == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(game);
    }

    @GetMapping("/history")
    public ResponseEntity<List<GameDto>> getUserGames(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(gameService.getUserGames(userId));
    }

    private Long extractUserId(Authentication authentication) {
        // Simplified; will be properly handled in WebSocket
        return 1L;
    }
}



