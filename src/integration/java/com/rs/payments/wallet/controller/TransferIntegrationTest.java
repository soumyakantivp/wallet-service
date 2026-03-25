package com.rs.payments.wallet.controller;

import com.rs.payments.wallet.BaseIntegrationTest;
import com.rs.payments.wallet.dto.CreateWalletRequest;
import com.rs.payments.wallet.dto.DepositRequest;
import com.rs.payments.wallet.dto.TransferRequest;
import com.rs.payments.wallet.model.Transaction;
import com.rs.payments.wallet.model.TransactionType;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.model.Wallet;
import com.rs.payments.wallet.repository.TransactionRepository;
import com.rs.payments.wallet.repository.UserRepository;
import com.rs.payments.wallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transfer Integration Tests")
class TransferIntegrationTest extends BaseIntegrationTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Should successfully transfer between wallets and verify balances and transactions")
    void shouldSuccessfullyTransferAndVerifyBalancesAndTransactions() {
        // Step 1: Create two users
        User sender = new User();
        sender.setUsername("sender");
        sender.setEmail("sender@example.com");
        sender = userRepository.save(sender);

        User receiver = new User();
        receiver.setUsername("receiver");
        receiver.setEmail("receiver@example.com");
        receiver = userRepository.save(receiver);

        // Step 2: Create wallets
        CreateWalletRequest walletRequest = new CreateWalletRequest();
        
        walletRequest.setUserId(sender.getId());
        String walletUrl = "http://localhost:" + port + "/wallets";
        ResponseEntity<Wallet> senderWalletResponse = restTemplate.postForEntity(walletUrl, walletRequest, Wallet.class);
        Wallet senderWallet = senderWalletResponse.getBody();
        assertThat(senderWallet).isNotNull();

        walletRequest.setUserId(receiver.getId());
        ResponseEntity<Wallet> receiverWalletResponse = restTemplate.postForEntity(walletUrl, walletRequest, Wallet.class);
        Wallet receiverWallet = receiverWalletResponse.getBody();
        assertThat(receiverWallet).isNotNull();

        // Step 3: Deposit to sender wallet
        BigDecimal depositAmount = new BigDecimal("500.00");
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(depositAmount);

        String depositUrl = "http://localhost:" + port + "/wallets/" + senderWallet.getId() + "/deposit";
        ResponseEntity<Wallet> depositResponse = restTemplate.postForEntity(depositUrl, depositRequest, Wallet.class);
        assertThat(depositResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(depositResponse.getBody().getBalance()).isEqualTo(new BigDecimal("500.00"));

        // Step 4: Transfer
        BigDecimal transferAmount = new BigDecimal("200.00");
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromWalletId(senderWallet.getId());
        transferRequest.setToWalletId(receiverWallet.getId());
        transferRequest.setAmount(transferAmount);

        String transferUrl = "http://localhost:" + port + "/transfers";
        ResponseEntity<Void> transferResponse = restTemplate.postForEntity(transferUrl, transferRequest, Void.class);
        assertThat(transferResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Step 5: Verify balances
        Wallet senderWalletAfter = walletRepository.findById(senderWallet.getId()).orElseThrow();
        assertThat(senderWalletAfter.getBalance()).isEqualTo(new BigDecimal("300.00"));

        Wallet receiverWalletAfter = walletRepository.findById(receiverWallet.getId()).orElseThrow();
        assertThat(receiverWalletAfter.getBalance()).isEqualTo(new BigDecimal("200.00"));

        // Step 6: Verify transactions
        List<Transaction> allTransactions = transactionRepository.findAll();
        assertThat(allTransactions).hasSize(4); // 1 deposit + 2 (transfer out) + 1 (transfer in)

        // Verify deposit transaction
        List<Transaction> depositTransactions = allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .toList();
        assertThat(depositTransactions).hasSize(1);
        assertThat(depositTransactions.get(0).getAmount()).isEqualTo(new BigDecimal("500.00"));
        assertThat(depositTransactions.get(0).getWallet().getId()).isEqualTo(senderWallet.getId());

        // Verify transfer out transaction
        List<Transaction> transferOutTransactions = allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_OUT)
                .toList();
        assertThat(transferOutTransactions).hasSize(1);
        assertThat(transferOutTransactions.get(0).getAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(transferOutTransactions.get(0).getWallet().getId()).isEqualTo(senderWallet.getId());

        // Verify transfer in transaction
        List<Transaction> transferInTransactions = allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_IN)
                .toList();
        assertThat(transferInTransactions).hasSize(1);
        assertThat(transferInTransactions.get(0).getAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(transferInTransactions.get(0).getWallet().getId()).isEqualTo(receiverWallet.getId());
    }

    @Test
    @DisplayName("Should return 400 when transferring with insufficient balance")
    void shouldReturn400ForInsufficientBalance() {
        // Step 1: Create two users and wallets
        User sender = new User();
        sender.setUsername("poorsender");
        sender.setEmail("poorsender@example.com");
        sender = userRepository.save(sender);

        User receiver = new User();
        receiver.setUsername("receiver2");
        receiver.setEmail("receiver2@example.com");
        receiver = userRepository.save(receiver);

        CreateWalletRequest walletRequest = new CreateWalletRequest();
        
        walletRequest.setUserId(sender.getId());
        String walletUrl = "http://localhost:" + port + "/wallets";
        ResponseEntity<Wallet> senderWalletResponse = restTemplate.postForEntity(walletUrl, walletRequest, Wallet.class);
        Wallet senderWallet = senderWalletResponse.getBody();

        walletRequest.setUserId(receiver.getId());
        ResponseEntity<Wallet> receiverWalletResponse = restTemplate.postForEntity(walletUrl, walletRequest, Wallet.class);
        Wallet receiverWallet = receiverWalletResponse.getBody();

        // Step 2: Deposit small amount
        BigDecimal depositAmount = new BigDecimal("50.00");
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(depositAmount);

        String depositUrl = "http://localhost:" + port + "/wallets/" + senderWallet.getId() + "/deposit";
        restTemplate.postForEntity(depositUrl, depositRequest, Wallet.class);

        // Step 3: Attempt transfer with insufficient balance
        BigDecimal transferAmount = new BigDecimal("200.00");
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromWalletId(senderWallet.getId());
        transferRequest.setToWalletId(receiverWallet.getId());
        transferRequest.setAmount(transferAmount);

        String transferUrl = "http://localhost:" + port + "/transfers";
        
        try {
            restTemplate.postForEntity(transferUrl, transferRequest, Void.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        // Verify balances did not change
        Wallet senderWalletAfter = walletRepository.findById(senderWallet.getId()).orElseThrow();
        assertThat(senderWalletAfter.getBalance()).isEqualTo(new BigDecimal("50.00"));

        Wallet receiverWalletAfter = walletRepository.findById(receiverWallet.getId()).orElseThrow();
        assertThat(receiverWalletAfter.getBalance()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return 400 when transferring to same wallet")
    void shouldReturn400ForSameWalletTransfer() {
        // Step 1: Create user and wallet
        User user = new User();
        user.setUsername("samewalletuser");
        user.setEmail("samewalletuser@example.com");
        user = userRepository.save(user);

        CreateWalletRequest walletRequest = new CreateWalletRequest();
        walletRequest.setUserId(user.getId());
        String walletUrl = "http://localhost:" + port + "/wallets";
        ResponseEntity<Wallet> walletResponse = restTemplate.postForEntity(walletUrl, walletRequest, Wallet.class);
        Wallet wallet = walletResponse.getBody();

        // Step 2: Deposit
        BigDecimal depositAmount = new BigDecimal("100.00");
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(depositAmount);

        String depositUrl = "http://localhost:" + port + "/wallets/" + wallet.getId() + "/deposit";
        restTemplate.postForEntity(depositUrl, depositRequest, Wallet.class);

        // Step 3: Attempt transfer to same wallet
        BigDecimal transferAmount = new BigDecimal("50.00");
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromWalletId(wallet.getId());
        transferRequest.setToWalletId(wallet.getId());
        transferRequest.setAmount(transferAmount);

        String transferUrl = "http://localhost:" + port + "/transfers";
        
        try {
            restTemplate.postForEntity(transferUrl, transferRequest, Void.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        // Verify balance did not change
        Wallet walletAfter = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(walletAfter.getBalance()).isEqualTo(new BigDecimal("100.00"));
    }
}
