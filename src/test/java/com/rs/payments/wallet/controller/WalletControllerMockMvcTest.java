package com.rs.payments.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rs.payments.wallet.dto.DepositRequest;
import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.dto.WithdrawRequest;
import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@DisplayName("Wallet Controller MockMvc Tests")
class WalletControllerMockMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private WalletService walletService;
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
    @Test
    @DisplayName("POST /wallets/{id}/deposit should successfully deposit amount")
    void shouldSuccessfullyDepositAmount() throws Exception {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal depositAmount = new BigDecimal("50.00");
        
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(new BigDecimal("150.00"));
        
        DepositRequest request = new DepositRequest();
        request.setAmount(depositAmount);
        
        when(walletService.deposit(walletId, depositAmount)).thenReturn(wallet);

        // When & Then
        mockMvc.perform(post("/wallets/{id}/deposit", walletId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(150.00));
        
        verify(walletService, times(1)).deposit(walletId, depositAmount);
    }

    @Test
    @DisplayName("POST /wallets/{id}/deposit should return 400 for invalid amount")
    void shouldReturnBadRequestForInvalidDepositAmount() throws Exception {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal invalidAmount = BigDecimal.ZERO;
        
        DepositRequest request = new DepositRequest();
        request.setAmount(invalidAmount);
        
        when(walletService.deposit(walletId, invalidAmount))
                .thenThrow(new IllegalArgumentException("Amount must be greater than 0"));

        // When & Then
        mockMvc.perform(post("/wallets/{id}/deposit", walletId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /wallets/{id}/deposit should return 404 when wallet not found")
    void shouldReturnNotFoundForDepositToNonExistentWallet() throws Exception {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal depositAmount = new BigDecimal("50.00");
        
        DepositRequest request = new DepositRequest();
        request.setAmount(depositAmount);
        
        when(walletService.deposit(walletId, depositAmount))
                .thenThrow(new ResourceNotFoundException("Wallet not found"));

        // When & Then
        mockMvc.perform(post("/wallets/{id}/deposit", walletId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /wallets/{id}/withdraw should successfully withdraw amount")
    void shouldSuccessfullyWithdrawAmount() throws Exception {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal withdrawAmount = new BigDecimal("50.00");
        
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(new BigDecimal("50.00"));
        
        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(withdrawAmount);
        
        when(walletService.withdraw(walletId, withdrawAmount)).thenReturn(wallet);

        // When & Then
        mockMvc.perform(post("/wallets/{id}/withdraw", walletId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(50.00));
        
        verify(walletService, times(1)).withdraw(walletId, withdrawAmount);
    }

    @Test
    @DisplayName("POST /wallets/{id}/withdraw should return 400 for insufficient balance")
    void shouldReturnBadRequestForInsufficientBalance() throws Exception {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal withdrawAmount = new BigDecimal("100.00");
        
        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(withdrawAmount);
        
        when(walletService.withdraw(walletId, withdrawAmount))
                .thenThrow(new IllegalArgumentException("Insufficient balance"));

        // When & Then
        mockMvc.perform(post("/wallets/{id}/withdraw", walletId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /wallets/{id}/withdraw should return 404 when wallet not found")
    void shouldReturnNotFoundForWithdrawFromNonExistentWallet() throws Exception {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal withdrawAmount = new BigDecimal("50.00");
        
        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(withdrawAmount);
        
        when(walletService.withdraw(walletId, withdrawAmount))
                .thenThrow(new ResourceNotFoundException("Wallet not found"));

        // When & Then
        mockMvc.perform(post("/wallets/{id}/withdraw", walletId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /wallets/transfer should successfully transfer between wallets")
    void shouldSuccessfullyTransferBetweenWallets() throws Exception {
        // Given
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        BigDecimal transferAmount = new BigDecimal("75.00");
        
        TransferRequest request = new TransferRequest();
        request.setFromWalletId(fromWalletId);
        request.setToWalletId(toWalletId);
        request.setAmount(transferAmount);
        
        doNothing().when(walletService).transfer(fromWalletId, toWalletId, transferAmount);

        // When & Then
        mockMvc.perform(post("/wallets/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
        
        verify(walletService, times(1)).transfer(fromWalletId, toWalletId, transferAmount);
    }

    @Test
    @DisplayName("POST /wallets/transfer should return 400 for insufficient balance")
    void shouldReturnBadRequestForTransferWithInsufficientBalance() throws Exception {
        // Given
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        BigDecimal transferAmount = new BigDecimal("150.00");
        
        TransferRequest request = new TransferRequest();
        request.setFromWalletId(fromWalletId);
        request.setToWalletId(toWalletId);
        request.setAmount(transferAmount);
        
        doThrow(new IllegalArgumentException("Insufficient balance"))
                .when(walletService).transfer(fromWalletId, toWalletId, transferAmount);

        // When & Then
        mockMvc.perform(post("/wallets/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /wallets/transfer should return 400 for same wallet transfer")
    void shouldReturnBadRequestForSameWalletTransfer() throws Exception {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal transferAmount = new BigDecimal("50.00");
        
        TransferRequest request = new TransferRequest();
        request.setFromWalletId(walletId);
        request.setToWalletId(walletId);
        request.setAmount(transferAmount);
        
        doThrow(new IllegalArgumentException("Cannot transfer to the same wallet"))
                .when(walletService).transfer(walletId, walletId, transferAmount);

        // When & Then
        mockMvc.perform(post("/wallets/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /wallets/transfer should return 404 when source wallet not found")
    void shouldReturnNotFoundForTransferFromNonExistentWallet() throws Exception {
        // Given
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        BigDecimal transferAmount = new BigDecimal("50.00");
        
        TransferRequest request = new TransferRequest();
        request.setFromWalletId(fromWalletId);
        request.setToWalletId(toWalletId);
        request.setAmount(transferAmount);
        
        doThrow(new ResourceNotFoundException("Source wallet not found"))
                .when(walletService).transfer(fromWalletId, toWalletId, transferAmount);

        // When & Then
        mockMvc.perform(post("/wallets/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance should return wallet balance")
    void shouldReturnWalletBalance() throws Exception {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal balance = new BigDecimal("250.50");
        
        when(walletService.getBalance(walletId)).thenReturn(balance);

        // When & Then
        mockMvc.perform(get("/wallets/{id}/balance", walletId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(250.50));
        
        verify(walletService, times(1)).getBalance(walletId);
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance should return 404 when wallet not found")
    void shouldReturnNotFoundForBalanceOfNonExistentWallet() throws Exception {
        // Given
        UUID walletId = UUID.randomUUID();
        
        when(walletService.getBalance(walletId))
                .thenThrow(new ResourceNotFoundException("Wallet not found"));

        // When & Then
        mockMvc.perform(get("/wallets/{id}/balance", walletId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
