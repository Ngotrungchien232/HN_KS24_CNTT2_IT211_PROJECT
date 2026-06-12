package com.project_cuoimon.service;

import com.project_cuoimon.dto.AccountResponse;
import com.project_cuoimon.dto.TransferRequest;
import com.project_cuoimon.entity.Account;
import com.project_cuoimon.entity.Transaction;
import com.project_cuoimon.entity.User;
import com.project_cuoimon.repository.AccountRepository;
import com.project_cuoimon.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User testUser;
    private Account sourceAccount;
    private Account targetAccount;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("customer1")
                .isKyc(true)
                .build();

        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("99900000001")
                .balance(BigDecimal.valueOf(50000))
                .currency("VND")
                .active(true)
                .transactionPin("encoded_pin")
                .user(testUser)
                .build();

        targetAccount = Account.builder()
                .id(2L)
                .accountNumber("99900000002")
                .balance(BigDecimal.valueOf(10000))
                .currency("VND")
                .active(true)
                .transactionPin("encoded_pin_target")
                .build();

        transferRequest = new TransferRequest();
        transferRequest.setTargetAccountNumber("99900000002");
        transferRequest.setAmount(BigDecimal.valueOf(20000));
        transferRequest.setDescription("Chuyen tien test");
        transferRequest.setTransactionPin("111222");
    }

    // Test Case 5: Xem số dư thành công
    @Test
    void getBalance_Success() {
        when(accountRepository.findByUser(testUser)).thenReturn(Optional.of(sourceAccount));

        AccountResponse response = accountService.getBalance(testUser);

        assertNotNull(response);
        assertEquals("99900000001", response.getAccountNumber());
        assertEquals(BigDecimal.valueOf(50000), response.getBalance());
        verify(accountRepository, times(1)).findByUser(testUser);
    }

    // Test Case 6: Lấy số dư thất bại do tài khoản bị khóa
    @Test
    void getBalance_AccountLocked_ThrowsException() {
        sourceAccount.setActive(false);
        when(accountRepository.findByUser(testUser)).thenReturn(Optional.of(sourceAccount));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.getBalance(testUser);
        });

        assertEquals("Tài khoản ngân hàng của bạn đang bị khóa!", exception.getMessage());
    }

    // Test Case 7: Chuyển tiền thành công giữa 2 tài khoản
    @Test
    void transfer_Success() {
        when(accountRepository.findByUser(testUser)).thenReturn(Optional.of(sourceAccount));
        when(passwordEncoder.matches("111222", "encoded_pin")).thenReturn(true);
        when(accountRepository.findByAccountNumber("99900000002")).thenReturn(Optional.of(targetAccount));
        
        // Mocking pessimistic lock
        when(accountRepository.findByAccountNumberWithLock("99900000001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberWithLock("99900000002")).thenReturn(Optional.of(targetAccount));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction tx = accountService.transfer(testUser, transferRequest);

        assertNotNull(tx);
        assertEquals(BigDecimal.valueOf(20000), tx.getAmount());
        assertEquals("SUCCESS", tx.getStatus());
        assertEquals(BigDecimal.valueOf(30000), sourceAccount.getBalance()); // 50000 - 20000
        assertEquals(BigDecimal.valueOf(30000), targetAccount.getBalance()); // 10000 + 20000

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    // Test Case 8: Chuyển tiền thất bại do nhập sai mã PIN
    @Test
    void transfer_WrongPin_ThrowsException() {
        when(accountRepository.findByUser(testUser)).thenReturn(Optional.of(sourceAccount));
        when(passwordEncoder.matches("111222", "encoded_pin")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.transfer(testUser, transferRequest);
        });

        assertEquals("Mã PIN giao dịch không chính xác!", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    // Test Case 9: Chuyển tiền thất bại do số dư không đủ
    @Test
    void transfer_InsufficientBalance_ThrowsException() {
        transferRequest.setAmount(BigDecimal.valueOf(60000)); // Lớn hơn số dư 50000

        when(accountRepository.findByUser(testUser)).thenReturn(Optional.of(sourceAccount));
        when(passwordEncoder.matches("111222", "encoded_pin")).thenReturn(true);
        when(accountRepository.findByAccountNumber("99900000002")).thenReturn(Optional.of(targetAccount));
        when(accountRepository.findByAccountNumberWithLock("99900000001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberWithLock("99900000002")).thenReturn(Optional.of(targetAccount));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.transfer(testUser, transferRequest);
        });

        assertEquals("Số dư tài khoản không đủ để thực hiện chuyển khoản!", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }
}
