package com.chess.controller;

import com.chess.dto.GameDto;
import com.chess.dto.InvitationDto;
import com.chess.dto.MoveDto;
import com.chess.dto.MoveRequest;
import com.chess.service.GameService;
import com.chess.service.InvitationService;
import com.chess.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket controller for real-time game events.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final GameService gameService;
    private final InvitationService invitationService;
    private final PresenceService presenceService;

    @MessageMapping("/lobby/presence")
    public void notifyPresence(SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserIdFromHeaders(headerAccessor);
        log.debug("Presence notification from user: {}", userId);
        presenceService.broadcastPresenceUpdate();
    }

    @MessageMapping("/invite")
    public void sendInvitation(@Payload Map<String, Object> payload, 
                              SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long fromUserId = getUserIdFromHeaders(headerAccessor);
            Long toUserId = Long.valueOf(payload.get("toUserId").toString());
            
            log.info("Invitation from {} to {}", fromUserId, toUserId);
            invitationService.sendInvitation(fromUserId, toUserId);
        } catch (Exception e) {
            log.error("Error sending invitation", e);
            throw e;
        }
    }

    @MessageMapping("/invitation/accept")
    public void acceptInvitation(@Payload Map<String, Object> payload,
                                SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserIdFromHeaders(headerAccessor);
        Long invitationId = Long.valueOf(payload.get("invitationId").toString());
        
        log.info("User {} accepting invitation {}", userId, invitationId);
        GameDto game = invitationService.acceptInvitation(invitationId, userId);
    }

    @MessageMapping("/invitation/decline")
    public void declineInvitation(@Payload Map<String, Object> payload,
                                  SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserIdFromHeaders(headerAccessor);
        Long invitationId = Long.valueOf(payload.get("invitationId").toString());
        
        log.info("User {} declining invitation {}", userId, invitationId);
        invitationService.declineInvitation(invitationId, userId);
    }

    @MessageMapping("/game/{gameId}/move")
    public void makeMove(@DestinationVariable Long gameId,
                        @Payload MoveRequest moveRequest,
                        SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long userId = getUserIdFromHeaders(headerAccessor);
            
            log.info("📥 MOVE REQUEST from user {} in game {}: {} -> {}", 
                     userId, gameId, moveRequest.getFrom(), moveRequest.getTo());
            
            MoveDto move = gameService.makeMove(gameId, userId, moveRequest);
            
            log.info("✅ MOVE PROCESSED successfully: Move ID {}", move.getId());
        } catch (Exception e) {
            log.error("❌ ERROR processing move in game {}: {}", gameId, e.getMessage(), e);
            throw e;
        }
    }

    @MessageMapping("/game/{gameId}/join")
    public void joinGame(@DestinationVariable Long gameId,
                        SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserIdFromHeaders(headerAccessor);
        log.info("User {} joined game {}", userId, gameId);
    }

    @MessageMapping("/game/{gameId}/resign")
    public void resignGame(@DestinationVariable Long gameId,
                          SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserIdFromHeaders(headerAccessor);
        log.info("User {} resigning from game {}", userId, gameId);
        gameService.resignGame(gameId, userId);
    }

    private Long getUserIdFromHeaders(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("userId")) {
            return (Long) sessionAttributes.get("userId");
        }
        throw new IllegalStateException("User ID not found in session");
    }
}


