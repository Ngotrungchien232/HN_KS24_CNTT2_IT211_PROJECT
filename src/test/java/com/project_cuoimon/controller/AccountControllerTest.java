package com.project_cuoimon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_cuoimon.dto.AccountResponse;
import com.project_cuoimon.dto.TransferRequest;
import com.project_cuoimon.entity.Account;
import com.project_cuoimon.entity.Transaction;
import com.project_cuoimon.entity.User;
import com.project_cuoimon.repository.UserRepository;
import com.project_cuoimon.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountService accountService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountController accountController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();
        objectMapper = new ObjectMapper();
    }

    // Test Case 6: Vấn tin số dư thành công
    @Test
    void getBalance_Success() throws Exception {
        User testUser = User.builder().id(1L).username("customer1").build();
        AccountResponse balanceResponse = AccountResponse.builder()
                .accountNumber("99900000001")
                .balance(BigDecimal.valueOf(50000))
                .currency("VND")
                .active(true)
                .build();

        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(accountService.getBalance(any(User.class))).thenReturn(balanceResponse);

        mockMvc.perform(get("/api/v1/customer/accounts/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vấn tin số dư tài khoản thành công!"))
                .andExpect(jsonPath("$.data.accountNumber").value("99900000001"))
                .andExpect(jsonPath("$.data.balance").value(50000));
    }

    // Test Case 7: Chuyển tiền thành công qua API
    @Test
    void transfer_Success() throws Exception {
        User testUser = User.builder().id(1L).username("customer1").build();
        TransferRequest request = new TransferRequest();
        request.setTargetAccountNumber("99900000002");
        request.setAmount(BigDecimal.valueOf(20000));
        request.setDescription("Chuyen tien");
        request.setTransactionPin("111222");

        Account fromAcc = Account.builder().accountNumber("99900000001").build();
        Account toAcc = Account.builder().accountNumber("99900000002").build();

        Transaction tx = Transaction.builder()
                .transactionCode("TX12345678")
                .amount(BigDecimal.valueOf(20000))
                .fromAccount(fromAcc)
                .toAccount(toAcc)
                .status("SUCCESS")
                .build();

        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(accountService.transfer(any(User.class), any(TransferRequest.class))).thenReturn(tx);

        mockMvc.perform(post("/api/v1/customer/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Giao dịch chuyển khoản thành công!"))
                .andExpect(jsonPath("$.data.transactionCode").value("TX12345678"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }
}
