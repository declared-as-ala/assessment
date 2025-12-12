package com.chess.service;

import com.chess.model.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChessValidationServiceTest {

    private ChessValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ChessValidationService();
    }

    @Test
    void validatePawnMoveForward_Success() {
        List<Move> moves = new ArrayList<>();
        
        String piece = validationService.validateAndGetPiece(moves, "e2", "e4", true, null);
        
        assertEquals("P", piece);
    }

    @Test
    void validatePawnMoveOneSquare_Success() {
        List<Move> moves = new ArrayList<>();
        
        String piece = validationService.validateAndGetPiece(moves, "e2", "e3", true, null);
        
        assertEquals("P", piece);
    }

    @Test
    void validateKnightMove_Success() {
        List<Move> moves = new ArrayList<>();
        
        String piece = validationService.validateAndGetPiece(moves, "b1", "c3", true, null);
        
        assertEquals("N", piece);
    }

    @Test
    void validateInvalidMove_ThrowsException() {
        List<Move> moves = new ArrayList<>();
        
        // Try to move pawn three squares
        assertThrows(IllegalArgumentException.class, () -> {
            validationService.validateAndGetPiece(moves, "e2", "e5", true, null);
        });
    }

    @Test
    void validateMovingOpponentPiece_ThrowsException() {
        List<Move> moves = new ArrayList<>();
        
        // White trying to move black's piece
        assertThrows(IllegalArgumentException.class, () -> {
            validationService.validateAndGetPiece(moves, "e7", "e6", true, null);
        });
    }

    @Test
    void validateEmptySquare_ThrowsException() {
        List<Move> moves = new ArrayList<>();
        
        // Try to move from empty square
        assertThrows(IllegalArgumentException.class, () -> {
            validationService.validateAndGetPiece(moves, "e4", "e5", true, null);
        });
    }
}



