package com.chess.service;

import com.chess.dto.UserDto;
import com.chess.model.User;
import com.chess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing user online presence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void markUserOnline(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(true);
            userRepository.save(user);
            broadcastPresenceUpdate();
            log.debug("User {} marked as online", userId);
        });
    }

    @Transactional
    public void markUserOffline(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(false);
            userRepository.save(user);
            broadcastPresenceUpdate();
            log.debug("User {} marked as offline", userId);
        });
    }

    public List<UserDto> getOnlineUsers() {
        return userRepository.findByOnlineTrue().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    public void broadcastPresenceUpdate() {
        List<UserDto> onlineUsers = getOnlineUsers();
        messagingTemplate.convertAndSend("/topic/presence", onlineUsers);
        log.debug("Broadcasted presence update: {} users online", onlineUsers.size());
    }
}



