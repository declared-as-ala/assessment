package com.chess.dto;

import com.chess.model.Invitation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDto {
    private Long id;
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private Long gameId;
    
    public static InvitationDto fromEntity(Invitation invitation) {
        return InvitationDto.builder()
                .id(invitation.getId())
                .fromUserId(invitation.getFromUserId())
                .toUserId(invitation.getToUserId())
                .status(invitation.getStatus().name())
                .createdAt(invitation.getCreatedAt())
                .respondedAt(invitation.getRespondedAt())
                .gameId(invitation.getGameId())
                .build();
    }
}



