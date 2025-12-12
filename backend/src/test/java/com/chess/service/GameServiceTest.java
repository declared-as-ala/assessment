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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MoveRepository moveRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ChessValidationService validationService;

    @InjectMocks
    private GameService gameService;

    private User whitePlayer;
    private User blackPlayer;
    private Game game;

    @BeforeEach
    void setUp() {
        whitePlayer = User.builder()
                .id(1L)
                .email("white@example.com")
                .displayName("White Player")
                .build();

        blackPlayer = User.builder()
                .id(2L)
                .email("black@example.com")
                .displayName("Black Player")
                .build();

        game = Game.builder()
                .id(1L)
                .whitePlayerId(1L)
                .blackPlayerId(2L)
                .status(Game.GameStatus.IN_PROGRESS)
                .currentTurn(Game.PlayerColor.WHITE)
                .moveCount(0)
                .build();
    }

    @Test
    void createGame_Success() {
        when(gameRepository.save(any(Game.class))).thenReturn(game);
        when(userRepository.findById(1L)).thenReturn(Optional.of(whitePlayer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(blackPlayer));

        GameDto result = gameService.createGame(1L, 2L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertNotNull(result.getWhitePlayerName());
        assertNotNull(result.getBlackPlayerName());

        verify(gameRepository).save(any(Game.class));
    }

    @Test
    void getGame_Success() {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(whitePlayer));
        when(moveRepository.findByGameIdOrderByMoveNumberAsc(1L)).thenReturn(new ArrayList<>());

        GameDto result = gameService.getGame(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(gameRepository).findById(1L);
        verify(moveRepository).findByGameIdOrderByMoveNumberAsc(1L);
    }

    @Test
    void makeMove_Success() {
        MoveRequest moveRequest = new MoveRequest();
        moveRequest.setFrom("e2");
        moveRequest.setTo("e4");

        Move move = Move.builder()
                .id(1L)
                .gameId(1L)
                .playerId(1L)
                .fromSquare("e2")
                .toSquare("e4")
                .piece("P")
                .san("e4")
                .moveNumber(1)
                .build();

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(moveRepository.findByGameIdOrderByMoveNumberAsc(1L)).thenReturn(new ArrayList<>());
        when(validationService.validateAndGetPiece(any(), anyString(), anyString(), anyBoolean(), any()))
                .thenReturn("P");
        when(validationService.getPieceAt(any(), anyString())).thenReturn(null);
        when(moveRepository.save(any(Move.class))).thenReturn(move);
        when(gameRepository.save(any(Game.class))).thenReturn(game);
        doNothing().when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));

        MoveDto result = gameService.makeMove(1L, 1L, moveRequest);

        assertNotNull(result);
        assertEquals("e2", result.getFromSquare());
        assertEquals("e4", result.getToSquare());

        verify(moveRepository).save(any(Move.class));
        verify(gameRepository).save(any(Game.class));
        verify(messagingTemplate).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void makeMove_NotPlayerTurn_ThrowsException() {
        MoveRequest moveRequest = new MoveRequest();
        moveRequest.setFrom("e7");
        moveRequest.setTo("e5");

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        // Black player trying to move when it's white's turn
        assertThrows(IllegalStateException.class, () -> {
            gameService.makeMove(1L, 2L, moveRequest);
        });

        verify(moveRepository, never()).save(any());
    }
}


