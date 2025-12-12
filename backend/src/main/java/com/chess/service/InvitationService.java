package com.chess.service;

import com.chess.dto.GameDto;
import com.chess.dto.InvitationDto;
import com.chess.model.Invitation;
import com.chess.model.Invitation.InvitationStatus;
import com.chess.repository.InvitationRepository;
import com.chess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling game invitations between players.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public InvitationDto sendInvitation(Long fromUserId, Long toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Cannot invite yourself");
        }

        var toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        Invitation invitation = Invitation.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .status(InvitationStatus.PENDING)
                .build();

        invitation = invitationRepository.save(invitation);
        log.info("Invitation sent from {} to {}", fromUserId, toUserId);

        InvitationDto dto = enrichInvitationDto(invitation);
        
        // Send to target user via WebSocket using email as principal
        messagingTemplate.convertAndSendToUser(
                toUser.getEmail(),
                "/queue/invitations",
                dto
        );

        return dto;
    }

    @Transactional
    public GameDto acceptInvitation(Long invitationId, Long userId) {
        Invitation invitation = invitationRepository.findByIdAndToUserId(invitationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation already processed");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());

        // Create game
        GameDto game = gameService.createGame(invitation.getFromUserId(), invitation.getToUserId());
        invitation.setGameId(game.getId());
        invitationRepository.save(invitation);

        log.info("Invitation {} accepted, game {} created", invitationId, game.getId());

        // Get user emails for WebSocket routing
        var fromUser = userRepository.findById(invitation.getFromUserId()).orElseThrow();
        var toUser = userRepository.findById(invitation.getToUserId()).orElseThrow();

        // Notify both players
        messagingTemplate.convertAndSendToUser(
                fromUser.getEmail(),
                "/queue/game-start",
                game
        );
        messagingTemplate.convertAndSendToUser(
                toUser.getEmail(),
                "/queue/game-start",
                game
        );

        return game;
    }

    @Transactional
    public void declineInvitation(Long invitationId, Long userId) {
        Invitation invitation = invitationRepository.findByIdAndToUserId(invitationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation already processed");
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        log.info("Invitation {} declined", invitationId);

        // Get sender email for WebSocket routing
        var fromUser = userRepository.findById(invitation.getFromUserId()).orElseThrow();

        // Notify sender
        messagingTemplate.convertAndSendToUser(
                fromUser.getEmail(),
                "/queue/invitation-declined",
                InvitationDto.fromEntity(invitation)
        );
    }

    @Transactional(readOnly = true)
    public List<InvitationDto> getPendingInvitations(Long userId) {
        List<Invitation> invitations = invitationRepository.findByToUserIdAndStatus(userId, InvitationStatus.PENDING);
        return invitations.stream()
                .map(this::enrichInvitationDto)
                .collect(Collectors.toList());
    }

    private InvitationDto enrichInvitationDto(Invitation invitation) {
        InvitationDto dto = InvitationDto.fromEntity(invitation);
        
        userRepository.findById(invitation.getFromUserId()).ifPresent(user -> 
            dto.setFromUserName(user.getDisplayName())
        );
        
        userRepository.findById(invitation.getToUserId()).ifPresent(user -> 
            dto.setToUserName(user.getDisplayName())
        );
        
        return dto;
    }
}


