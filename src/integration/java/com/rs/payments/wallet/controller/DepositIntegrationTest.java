package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.BaseIntegrationTest;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.dto.DepositRequest;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Deposit Integration Tests")
class DepositIntegrationTest extends BaseIntegrationTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    @DisplayName("Should successfully deposit to wallet and verify balance")
    void shouldSuccessfullyDepositAndVerifyBalance() {
        // Step 1: Create user
        User user = new User();
        user.setUsername("deposituser");
        user.setEmail("deposittest@example.com");
        user = userRepository.save(user);

        // Step 2: Create wallet
        CreateWalletRequest walletRequest = new CreateWalletRequest();
        walletRequest.setUserId(user.getId());

        String walletUrl = "http://localhost:" + port + "/wallets";
        ResponseEntity<Wallet> walletResponse = restTemplate.postForEntity(walletUrl, walletRequest, Wallet.class);

        assertThat(walletResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(walletResponse.getBody()).isNotNull();
        Wallet wallet = walletResponse.getBody();
        assertThat(wallet.getId()).isNotNull();
        assertThat(wallet.getBalance()).isEqualTo(BigDecimal.ZERO);

        // Step 3: Deposit
        BigDecimal depositAmount = new BigDecimal("150.75");
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(depositAmount);

        String depositUrl = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/deposit";
        ResponseEntity<Wallet> depositResponse = restTemplate.postForEntity(depositUrl, depositRequest, Wallet.class);

        assertThat(depositResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(depositResponse.getBody()).isNotNull();

        // Step 4: Verify balance
        Wallet updatedWallet = depositResponse.getBody();
        assertThat(updatedWallet.getBalance()).isEqualTo(new BigDecimal("150.75"));

        // Verify in database
        Wallet walletFromDb = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(walletFromDb.getBalance()).isEqualTo(new BigDecimal("150.75"));
    }

    @Test
    @DisplayName("Should deposit multiple times and accumulate balance")
    void shouldAccumulateBalanceOnMultipleDeposits() {
        // Step 1: Create user
        User user = new User();
        user.setUsername("multiuser");
        user.setEmail("multi@example.com");
        user = userRepository.save(user);

        // Step 2: Create wallet
        CreateWalletRequest walletRequest = new CreateWalletRequest();
        walletRequest.setUserId(user.getId());

        String walletUrl = "http://localhost:" + port + "/wallets";
        ResponseEntity<Wallet> walletResponse = restTemplate.postForEntity(walletUrl, walletRequest, Wallet.class);

        Wallet wallet = walletResponse.getBody();
        assertThat(wallet).isNotNull();

        // Step 3: First deposit
        BigDecimal firstDeposit = new BigDecimal("100.00");
        DepositRequest firstDepositRequest = new DepositRequest();
        firstDepositRequest.setAmount(firstDeposit);

        String depositUrl = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/deposit";
        ResponseEntity<Wallet> firstDepositResponse = restTemplate.postForEntity(depositUrl, firstDepositRequest, Wallet.class);

        assertThat(firstDepositResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstDepositResponse.getBody().getBalance()).isEqualTo(new BigDecimal("100.00"));

        // Step 4: Second deposit
        BigDecimal secondDeposit = new BigDecimal("50.50");
        DepositRequest secondDepositRequest = new DepositRequest();
        secondDepositRequest.setAmount(secondDeposit);

        ResponseEntity<Wallet> secondDepositResponse = restTemplate.postForEntity(depositUrl, secondDepositRequest, Wallet.class);

        assertThat(secondDepositResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondDepositResponse.getBody().getBalance()).isEqualTo(new BigDecimal("150.50"));

        // Step 5: Verify final balance in database
        Wallet walletFromDb = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(walletFromDb.getBalance()).isEqualTo(new BigDecimal("150.50"));
    }

    @Test
    @DisplayName("Should return 404 for deposit to non-existent wallet")
    void shouldReturn404ForNonExistentWallet() {
        // Attempt to deposit to non-existent wallet
        BigDecimal depositAmount = new BigDecimal("100.00");
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(depositAmount);

        String depositUrl = "http://localhost:" + port + "/wallets/" + java.util.UUID.randomUUID() + "/deposit";

        try {
            restTemplate.postForEntity(depositUrl, depositRequest, Wallet.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
