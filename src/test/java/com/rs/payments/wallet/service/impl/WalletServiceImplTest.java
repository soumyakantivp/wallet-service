package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.exception.ResourceNotFoundException;
import com.rs.payments.wallet.model.Transaction;
import com.rs.payments.wallet.model.TransactionType;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    @DisplayName("Should create wallet for existing user")
    void shouldCreateWalletForExistingUser() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // The service saves the user, which cascades to wallet. 
        // We mock save to return the user.
        when(userRepository.save(user)).thenReturn(user);

        // When
        Wallet result = walletService.createWalletForUser(userId);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertEquals(walletService.createWalletForUser(userId).getBalance(), BigDecimal.ZERO);
        
        // Verify interactions
        verify(userRepository, times(2)).findById(userId); // Called twice due to second assert
        verify(userRepository, times(2)).save(user);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> walletService.createWalletForUser(userId));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should increase wallet balance on valid deposit")
    void shouldIncreaseWalletBalanceOnValidDeposit() {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal currentBalance = new BigDecimal("100.00");
        BigDecimal depositAmount = new BigDecimal("50.00");
        
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(currentBalance);
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);

        // When
        Wallet result = walletService.deposit(walletId, depositAmount);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("150.00"), result.getBalance());
        verify(walletRepository, times(1)).findById(walletId);
        verify(walletRepository, times(1)).save(wallet);
        
        // Verify transaction creation
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(transactionCaptor.capture());
        
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(TransactionType.DEPOSIT, savedTransaction.getType());
        assertEquals(depositAmount, savedTransaction.getAmount());
        assertEquals(walletId, savedTransaction.getWallet().getId());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for amount <= 0")
    void shouldThrowIllegalArgumentExceptionForInvalidAmount() {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal invalidAmount = BigDecimal.ZERO;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> walletService.deposit(walletId, invalidAmount));
        verify(walletRepository, never()).findById(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException if wallet not found")
    void shouldThrowResourceNotFoundExceptionWhenWalletNotFound() {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal depositAmount = new BigDecimal("50.00");
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> walletService.deposit(walletId, depositAmount));
        verify(walletRepository, times(1)).findById(walletId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reduce wallet balance on successful withdrawal")
    void shouldReduceWalletBalanceOnSuccessfulWithdrawal() {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal currentBalance = new BigDecimal("100.00");
        BigDecimal withdrawAmount = new BigDecimal("50.00");
        
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(currentBalance);
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);

        // When
        Wallet result = walletService.withdraw(walletId, withdrawAmount);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("50.00"), result.getBalance());
        verify(walletRepository, times(1)).findById(walletId);
        verify(walletRepository, times(1)).save(wallet);
        
        // Verify transaction creation
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(transactionCaptor.capture());
        
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(TransactionType.WITHDRAWAL, savedTransaction.getType());
        assertEquals(withdrawAmount, savedTransaction.getAmount());
        assertEquals(walletId, savedTransaction.getWallet().getId());
    }

    @Test
    @DisplayName("Should throw exception for insufficient balance")
    void shouldThrowExceptionForInsufficientBalance() {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal currentBalance = new BigDecimal("30.00");
        BigDecimal withdrawAmount = new BigDecimal("50.00");
        
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setBalance(currentBalance);
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> walletService.withdraw(walletId, withdrawAmount));
        verify(walletRepository, times(1)).findById(walletId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for amount <= 0")
    void shouldThrowIllegalArgumentExceptionForInvalidWithdrawAmount() {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal invalidAmount = BigDecimal.ZERO;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> walletService.withdraw(walletId, invalidAmount));
        verify(walletRepository, never()).findById(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException if wallet not found for withdrawal")
    void shouldThrowResourceNotFoundExceptionWhenWalletNotFoundForWithdrawal() {
        // Given
        UUID walletId = UUID.randomUUID();
        BigDecimal withdrawAmount = new BigDecimal("50.00");
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> walletService.withdraw(walletId, withdrawAmount));
        verify(walletRepository, times(1)).findById(walletId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
