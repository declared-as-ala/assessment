package com.chess.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private String message;
    private String details;
    private int status;
    
    public static ErrorResponse of(String message, String details, int status) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .message(message)
                .details(details)
                .status(status)
                .build();
    }
}



