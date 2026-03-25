package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.dto.DepositRequest;
import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.dto.WithdrawRequest;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/wallets")
@Tag(name = "Wallet Management", description = "APIs for managing user wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(
            summary = "Create a new wallet for a user",
            description = "Creates a new wallet for the specified user ID with a zero balance.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Wallet created successfully",
                            content = @Content(schema = @Schema(implementation = Wallet.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found"
                    )
            }
    )
    @PostMapping
    public ResponseEntity<Wallet> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWalletForUser(request.getUserId());
        return ResponseEntity.ok(wallet);
    }

    @Operation(
            summary = "Deposit amount to wallet",
            description = "Adds the specified amount to a wallet's balance.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Deposit successful",
                            content = @Content(schema = @Schema(implementation = Wallet.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid amount"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Wallet not found"
                    )
            }
    )
    @PostMapping("/{id}/deposit")
    public ResponseEntity<Wallet> deposit(
            @PathVariable UUID id,
            @Valid @RequestBody DepositRequest request) {
        Wallet wallet = walletService.deposit(id, request.getAmount());
        return ResponseEntity.ok(wallet);
    }

    @Operation(
            summary = "Withdraw amount from wallet",
            description = "Deducts the specified amount from a wallet's balance.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Withdrawal successful",
                            content = @Content(schema = @Schema(implementation = Wallet.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid amount or insufficient balance"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Wallet not found"
                    )
            }
    )
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Wallet> withdraw(
            @PathVariable UUID id,
            @Valid @RequestBody WithdrawRequest request) {
        Wallet wallet = walletService.withdraw(id, request.getAmount());
        return ResponseEntity.ok(wallet);
    }

    @Operation(
            summary = "Get wallet balance",
            description = "Retrieves the current balance of a wallet.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Balance retrieved successfully"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Wallet not found"
                    )
            }
    )
    @GetMapping("/{id}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable UUID id) {
        BigDecimal balance = walletService.getBalance(id);
        return ResponseEntity.ok(balance);
    }

    @Operation(
            summary = "Transfer amount between wallets",
            description = "Transfers the specified amount from one wallet to another.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Transfer successful"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid amount, insufficient balance, or same wallet transfer"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "One or both wallets not found"
                    )
            }
    )
    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
        walletService.transfer(request.getFromWalletId(), request.getToWalletId(), request.getAmount());
        return ResponseEntity.noContent().build();
    }
}