package com.project_cuoimon.service;

import com.project_cuoimon.dto.AccountResponse;
import com.project_cuoimon.dto.TransferRequest;
import com.project_cuoimon.dto.TransactionResponseDto;
import com.project_cuoimon.dto.ChangePinRequest;
import com.project_cuoimon.entity.Transaction;
import com.project_cuoimon.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {
    // Vấn tin số dư tài khoản của người dùng (FR-06)
    AccountResponse getBalance(User user);

    // Chuyển tiền nội bộ/liên ngân hàng (FR-07)
    Transaction transfer(User user, TransferRequest request);

    // Xem sao kê lịch sử giao dịch (FR-08)
    Page<TransactionResponseDto> getStatement(User user, Pageable pageable);

    // Đổi mã PIN giao dịch (FR-10)
    void changePin(User user, ChangePinRequest request);
}