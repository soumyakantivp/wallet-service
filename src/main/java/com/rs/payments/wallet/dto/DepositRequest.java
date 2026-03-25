package com.rs.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request to deposit amount to wallet")
public class DepositRequest {
    @NotNull
    @Positive
    @Schema(description = "Amount to deposit", example = "50.00")
    private BigDecimal amount;
}
