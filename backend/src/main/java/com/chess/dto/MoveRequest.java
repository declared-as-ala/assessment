package com.chess.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MoveRequest {
    
    @NotBlank(message = "From square is required")
    @Pattern(regexp = "[a-h][1-8]", message = "From square must be in format like 'e2'")
    private String from;
    
    @NotBlank(message = "To square is required")
    @Pattern(regexp = "[a-h][1-8]", message = "To square must be in format like 'e4'")
    private String to;
    
    private String promotion; // For pawn promotion: Q, R, B, N
}



