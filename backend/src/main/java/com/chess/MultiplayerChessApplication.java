package com.chess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for Multiplayer Chess Backend.
 * 
 * This application provides:
 * - REST API for authentication, lobby, and game management
 * - WebSocket (STOMP) for real-time game events and presence
 * - JWT-based security
 * - H2/PostgreSQL persistence
 */
@SpringBootApplication
public class MultiplayerChessApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiplayerChessApplication.class, args);
    }
}



