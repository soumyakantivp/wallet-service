package com.rs.payments.wallet.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Error response")
public class ErrorResponse {
    @Schema(description = "HTTP status code")
    private int status;

    @Schema(description = "Error message")
    private String message;

    @Schema(description = "Timestamp when error occurred")
    private long timestamp;
}
