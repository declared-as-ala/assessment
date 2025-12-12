package com.chess.service;

import com.chess.model.Move;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for validating chess moves according to piece movement rules.
 * Implements basic chess rules (piece movement, turn order).
 * Does not implement: check/checkmate, en passant, castling (optional features).
 */
@Service
@Slf4j
public class ChessValidationService {

    private static final String INITIAL_BOARD = 
        "rnbqkbnr" +  // rank 8
        "pppppppp" +  // rank 7
        "        " +  // rank 6
        "        " +  // rank 5
        "        " +  // rank 4
        "        " +  // rank 3
        "PPPPPPPP" +  // rank 2
        "RNBQKBNR";   // rank 1

    /**
     * Validates a move and returns the piece being moved.
     * Throws IllegalArgumentException if move is invalid.
     */
    public String validateAndGetPiece(List<Move> previousMoves, String from, String to, 
                                      boolean isWhiteTurn, String promotion) {
        
        Map<String, String> board = reconstructBoard(previousMoves);
        
        String piece = board.get(from);
        if (piece == null || piece.equals(" ")) {
            throw new IllegalArgumentException("No piece at source square");
        }
        
        boolean isPieceWhite = Character.isUpperCase(piece.charAt(0));
        if (isPieceWhite != isWhiteTurn) {
            throw new IllegalArgumentException("Not your piece");
        }
        
        String targetPiece = board.get(to);
        if (targetPiece != null && !targetPiece.equals(" ")) {
            boolean isTargetWhite = Character.isUpperCase(targetPiece.charAt(0));
            if (isTargetWhite == isPieceWhite) {
                throw new IllegalArgumentException("Cannot capture your own piece");
            }
        }
        
        // Validate move pattern for piece type
        if (!isValidMovePattern(piece, from, to, board)) {
            throw new IllegalArgumentException("Invalid move for " + piece);
        }
        
        // Handle pawn promotion
        if ((piece.equalsIgnoreCase("P")) && (to.charAt(1) == '8' || to.charAt(1) == '1')) {
            if (promotion == null || !isValidPromotion(promotion)) {
                throw new IllegalArgumentException("Pawn promotion required");
            }
        }
        
        return piece.toUpperCase();
    }

    /**
     * Get the piece at a given square based on move history.
     */
    public String getPieceAt(List<Move> previousMoves, String square) {
        Map<String, String> board = reconstructBoard(previousMoves);
        String piece = board.get(square);
        return (piece != null && !piece.equals(" ")) ? piece : null;
    }

    /**
     * Reconstructs the current board state from initial position + moves.
     */
    private Map<String, String> reconstructBoard(List<Move> moves) {
        Map<String, String> board = new HashMap<>();
        
        // Initialize board
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                char fileChar = (char) ('a' + file);
                char rankChar = (char) ('1' + rank);
                String square = "" + fileChar + rankChar;
                int index = (7 - rank) * 8 + file;
                board.put(square, String.valueOf(INITIAL_BOARD.charAt(index)));
            }
        }
        
        // Apply moves
        for (Move move : moves) {
            String piece = move.getPiece();
            if (move.getPromotion() != null) {
                piece = move.getPromotion();
            }
            
            // Adjust case based on player
            boolean isWhite = move.getMoveNumber() % 2 == 1; // Odd moves are white
            piece = isWhite ? piece.toUpperCase() : piece.toLowerCase();
            
            board.put(move.getFromSquare(), " ");
            board.put(move.getToSquare(), piece);
        }
        
        return board;
    }

    /**
     * Validates if a piece can move from 'from' to 'to' according to chess rules.
     */
    private boolean isValidMovePattern(String piece, String from, String to, Map<String, String> board) {
        char pieceType = Character.toUpperCase(piece.charAt(0));
        
        int fromFile = from.charAt(0) - 'a';
        int fromRank = from.charAt(1) - '1';
        int toFile = to.charAt(0) - 'a';
        int toRank = to.charAt(1) - '1';
        
        int fileDiff = Math.abs(toFile - fromFile);
        int rankDiff = Math.abs(toRank - fromRank);
        
        boolean isCapture = board.get(to) != null && !board.get(to).equals(" ");
        
        switch (pieceType) {
            case 'P': // Pawn
                return isValidPawnMove(from, to, piece, isCapture, fromFile, fromRank, toFile, toRank, fileDiff, rankDiff);
            
            case 'N': // Knight
                return (fileDiff == 2 && rankDiff == 1) || (fileDiff == 1 && rankDiff == 2);
            
            case 'B': // Bishop
                return fileDiff == rankDiff && fileDiff > 0 && isPathClear(from, to, board);
            
            case 'R': // Rook
                return (fileDiff == 0 || rankDiff == 0) && isPathClear(from, to, board);
            
            case 'Q': // Queen
                return ((fileDiff == rankDiff) || (fileDiff == 0 || rankDiff == 0)) 
                        && isPathClear(from, to, board);
            
            case 'K': // King
                return fileDiff <= 1 && rankDiff <= 1 && (fileDiff + rankDiff > 0);
            
            default:
                return false;
        }
    }

    private boolean isValidPawnMove(String from, String to, String piece, boolean isCapture,
                                   int fromFile, int fromRank, int toFile, int toRank,
                                   int fileDiff, int rankDiff) {
        boolean isWhite = Character.isUpperCase(piece.charAt(0));
        int direction = isWhite ? 1 : -1;
        int startRank = isWhite ? 1 : 6;
        
        // Forward move (no capture)
        if (!isCapture && fileDiff == 0) {
            if (rankDiff == 1 && toRank == fromRank + direction) {
                return true;
            }
            // Initial two-square move
            if (fromRank == startRank && rankDiff == 2 && toRank == fromRank + 2 * direction) {
                return true;
            }
        }
        
        // Diagonal capture
        if (isCapture && fileDiff == 1 && rankDiff == 1 && toRank == fromRank + direction) {
            return true;
        }
        
        return false;
    }

    /**
     * Checks if the path between two squares is clear (for sliding pieces).
     */
    private boolean isPathClear(String from, String to, Map<String, String> board) {
        int fromFile = from.charAt(0) - 'a';
        int fromRank = from.charAt(1) - '1';
        int toFile = to.charAt(0) - 'a';
        int toRank = to.charAt(1) - '1';
        
        int fileStep = Integer.compare(toFile, fromFile);
        int rankStep = Integer.compare(toRank, fromRank);
        
        int currentFile = fromFile + fileStep;
        int currentRank = fromRank + rankStep;
        
        while (currentFile != toFile || currentRank != toRank) {
            String square = "" + (char)('a' + currentFile) + (char)('1' + currentRank);
            String piece = board.get(square);
            if (piece != null && !piece.equals(" ")) {
                return false;
            }
            currentFile += fileStep;
            currentRank += rankStep;
        }
        
        return true;
    }

    private boolean isValidPromotion(String promotion) {
        return promotion != null && (promotion.equals("Q") || promotion.equals("R") || 
                                    promotion.equals("B") || promotion.equals("N"));
    }
}



