package com.rs.payments.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "Request to transfer amount between wallets")
public class TransferRequest {
    @NotNull
    @Schema(description = "Source wallet ID", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID fromWalletId;

    @NotNull
    @Schema(description = "Destination wallet ID", example = "b1f8e321-7c9b-46e2-8d1a-4f5a6b7c8d9e")
    private UUID toWalletId;

    @NotNull
    @Positive
    @Schema(description = "Amount to transfer", example = "50.00")
    private BigDecimal amount;
}
