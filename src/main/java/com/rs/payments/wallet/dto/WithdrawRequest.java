package com.rs.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request to withdraw amount from wallet")
public class WithdrawRequest {
    @NotNull
    @Positive
    @Schema(description = "Amount to withdraw", example = "50.00")
    private BigDecimal amount;
}
