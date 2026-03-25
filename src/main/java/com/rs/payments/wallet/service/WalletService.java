package com.rs.payments.wallet.service;

import java.util.UUID;
import com.rs.payments.wallet.model.Wallet;
import java.math.BigDecimal;

public interface WalletService {
    Wallet createWalletForUser(UUID userId);
    
    Wallet deposit(UUID walletId, BigDecimal amount);
    
    Wallet withdraw(UUID walletId, BigDecimal amount);
    
    BigDecimal getBalance(UUID walletId);
    
    void transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount);
}