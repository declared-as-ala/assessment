package com.chess.repository;

import com.chess.model.Game;
import com.chess.model.Game.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    
    @Query("SELECT g FROM Game g WHERE (g.whitePlayerId = :userId OR g.blackPlayerId = :userId) AND g.status = :status")
    List<Game> findByPlayerIdAndStatus(Long userId, GameStatus status);
    
    @Query("SELECT g FROM Game g WHERE (g.whitePlayerId = :userId OR g.blackPlayerId = :userId) ORDER BY g.createdAt DESC")
    List<Game> findByPlayerId(Long userId);
    
    Optional<Game> findFirstByWhitePlayerIdOrBlackPlayerIdAndStatus(Long whitePlayerId, Long blackPlayerId, GameStatus status);
}



