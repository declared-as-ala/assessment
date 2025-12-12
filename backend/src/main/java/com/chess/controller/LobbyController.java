package com.chess.controller;

import com.chess.dto.InvitationDto;
import com.chess.dto.UserDto;
import com.chess.service.InvitationService;
import com.chess.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for lobby operations (online players, invitations).
 */
@RestController
@RequestMapping("/api/lobby")
@RequiredArgsConstructor
public class LobbyController {

    private final PresenceService presenceService;
    private final InvitationService invitationService;

    @GetMapping("/players")
    public ResponseEntity<List<UserDto>> getOnlinePlayers() {
        return ResponseEntity.ok(presenceService.getOnlineUsers());
    }

    @GetMapping("/invitations")
    public ResponseEntity<List<InvitationDto>> getPendingInvitations(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(invitationService.getPendingInvitations(userId));
    }

    private Long extractUserId(Authentication authentication) {
        // This is a simplified approach; in production, extract from JWT or UserDetails
        return 1L; // Will be properly extracted in WebSocketController
    }
}



