package com.chess.service;

import com.chess.dto.GameDto;
import com.chess.dto.MoveDto;
import com.chess.dto.MoveRequest;
import com.chess.model.Game;
import com.chess.model.Move;
import com.chess.model.User;
import com.chess.repository.GameRepository;
import com.chess.repository.MoveRepository;
import com.chess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Service handling game logic and move management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChessValidationService validationService;

    @Transactional
    public GameDto createGame(Long player1Id, Long player2Id) {
        // Randomly assign white and black
        Random random = new Random();
        boolean player1IsWhite = random.nextBoolean();

        Game game = Game.builder()
                .whitePlayerId(player1IsWhite ? player1Id : player2Id)
                .blackPlayerId(player1IsWhite ? player2Id : player1Id)
                .status(Game.GameStatus.IN_PROGRESS)
                .currentTurn(Game.PlayerColor.WHITE)
                .moveCount(0)
                .build();

        game = gameRepository.save(game);
        log.info("Game created: {} (White: {}, Black: {})", game.getId(), 
                 game.getWhitePlayerId(), game.getBlackPlayerId());

        return enrichGameDto(game);
    }

    @Transactional(readOnly = true)
    public GameDto getGame(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
        
        GameDto gameDto = enrichGameDto(game);
        
        // Load moves
        List<MoveDto> moves = moveRepository.findByGameIdOrderByMoveNumberAsc(gameId).stream()
                .map(MoveDto::fromEntity)
                .collect(Collectors.toList());
        
        gameDto.setMoves(moves);
        return gameDto;
    }

    @Transactional
    public MoveDto makeMove(Long gameId, Long userId, MoveRequest moveRequest) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        if (game.getStatus() != Game.GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }

        // Verify it's the player's turn
        boolean isWhiteTurn = game.getCurrentTurn() == Game.PlayerColor.WHITE;
        boolean isWhitePlayer = game.getWhitePlayerId().equals(userId);

        if (isWhiteTurn != isWhitePlayer) {
            throw new IllegalStateException("It's not your turn");
        }

        // Get current board state
        List<Move> previousMoves = moveRepository.findByGameIdOrderByMoveNumberAsc(gameId);
        
        // Validate move using chess rules
        String piece = validationService.validateAndGetPiece(
                previousMoves, moveRequest.getFrom(), moveRequest.getTo(), isWhiteTurn, moveRequest.getPromotion()
        );

        // Check if capture occurred
        String capturedPiece = validationService.getPieceAt(previousMoves, moveRequest.getTo());

        // Create move
        Move move = Move.builder()
                .gameId(gameId)
                .playerId(userId)
                .fromSquare(moveRequest.getFrom())
                .toSquare(moveRequest.getTo())
                .piece(piece)
                .capturedPiece(capturedPiece)
                .promotion(moveRequest.getPromotion())
                .san(generateSAN(piece, moveRequest, capturedPiece != null))
                .moveNumber(game.getMoveCount() + 1)
                .build();

        move = moveRepository.save(move);

        // Update game
        game.setMoveCount(game.getMoveCount() + 1);
        game.setCurrentTurn(isWhiteTurn ? Game.PlayerColor.BLACK : Game.PlayerColor.WHITE);
        gameRepository.save(game);

        log.info("Move made in game {}: {} -> {}", gameId, moveRequest.getFrom(), moveRequest.getTo());

        MoveDto moveDto = MoveDto.fromEntity(move);
        
        // Broadcast move to all subscribers
        String topic = "/topic/game/" + gameId + "/moves";
        messagingTemplate.convertAndSend(topic, moveDto);
        log.info("✅ Move broadcasted to topic: {} with move ID: {}", topic, moveDto.getId());

        return moveDto;
    }

    @Transactional(readOnly = true)
    public List<MoveDto> getGameMoves(Long gameId, Long lastMoveId) {
        List<Move> moves;
        if (lastMoveId != null && lastMoveId > 0) {
            moves = moveRepository.findByGameIdAndIdGreaterThanOrderByMoveNumberAsc(gameId, lastMoveId);
        } else {
            moves = moveRepository.findByGameIdOrderByMoveNumberAsc(gameId);
        }
        
        return moves.stream()
                .map(MoveDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GameDto> getUserGames(Long userId) {
        List<Game> games = gameRepository.findByPlayerId(userId);
        return games.stream()
                .map(this::enrichGameDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GameDto getActiveGame(Long userId) {
        List<Game> activeGames = gameRepository.findByPlayerIdAndStatus(userId, Game.GameStatus.IN_PROGRESS);
        if (activeGames.isEmpty()) {
            return null;
        }
        return enrichGameDto(activeGames.get(0));
    }

    private GameDto enrichGameDto(Game game) {
        GameDto dto = GameDto.fromEntity(game);
        
        userRepository.findById(game.getWhitePlayerId()).ifPresent(user -> 
            dto.setWhitePlayerName(user.getDisplayName())
        );
        
        userRepository.findById(game.getBlackPlayerId()).ifPresent(user -> 
            dto.setBlackPlayerName(user.getDisplayName())
        );
        
        return dto;
    }

    private String generateSAN(String piece, MoveRequest moveRequest, boolean isCapture) {
        StringBuilder san = new StringBuilder();
        
        if (!"P".equals(piece)) {
            san.append(piece);
        }
        
        if (isCapture) {
            if ("P".equals(piece)) {
                san.append(moveRequest.getFrom().charAt(0)); // file of pawn
            }
            san.append("x");
        }
        
        san.append(moveRequest.getTo());
        
        if (moveRequest.getPromotion() != null) {
            san.append("=").append(moveRequest.getPromotion());
        }
        
        return san.toString();
    }

    @Transactional
    public GameDto resignGame(Long gameId, Long userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        if (game.getStatus() != Game.GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }

        // Determine winner (the other player)
        Long winnerId;
        String resignedPlayerName;
        String winnerPlayerName;
        
        if (game.getWhitePlayerId().equals(userId)) {
            winnerId = game.getBlackPlayerId();
            resignedPlayerName = userRepository.findById(userId)
                    .map(u -> u.getDisplayName()).orElse("White");
            winnerPlayerName = userRepository.findById(winnerId)
                    .map(u -> u.getDisplayName()).orElse("Black");
        } else if (game.getBlackPlayerId().equals(userId)) {
            winnerId = game.getWhitePlayerId();
            resignedPlayerName = userRepository.findById(userId)
                    .map(u -> u.getDisplayName()).orElse("Black");
            winnerPlayerName = userRepository.findById(winnerId)
                    .map(u -> u.getDisplayName()).orElse("White");
        } else {
            throw new IllegalArgumentException("You are not a player in this game");
        }

        // Mark game as finished to prevent any further moves
        game.setStatus(Game.GameStatus.COMPLETED);
        game.setWinnerId(winnerId);
        game.setCompletedAt(java.time.LocalDateTime.now());
        game = gameRepository.save(game);

        log.info("Game {} resigned by user {} ({}). Winner: {} ({})", 
                 gameId, userId, resignedPlayerName, winnerId, winnerPlayerName);

        // Create enriched DTO with all info
        GameDto gameDto = enrichGameDto(game);

        // Broadcast to ALL subscribers of this game
        String resignTopic = "/topic/game/" + gameId + "/resigned";
        messagingTemplate.convertAndSend(resignTopic, gameDto);
        
        log.info("✅ RESIGNATION BROADCASTED to topic: {} | Winner: {} | Status: {}", 
                 resignTopic, gameDto.getWinnerId(), gameDto.getStatus());

        return gameDto;
    }
}


