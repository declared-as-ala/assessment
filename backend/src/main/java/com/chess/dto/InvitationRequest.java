package com.chess.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InvitationRequest {
    
    @NotNull(message = "Target user ID is required")
    private Long toUserId;
}



