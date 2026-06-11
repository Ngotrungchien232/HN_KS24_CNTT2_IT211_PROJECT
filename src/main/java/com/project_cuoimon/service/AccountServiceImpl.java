package com.project_cuoimon.service;

import com.project_cuoimon.dto.*;
import com.project_cuoimon.entity.Account;
import com.project_cuoimon.entity.Transaction;
import com.project_cuoimon.entity.User;
import com.project_cuoimon.repository.AccountRepository;
import com.project_cuoimon.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public AccountResponse getBalance(User user) {
        // Tìm kiếm tài khoản liên kết với User đang đăng nhập
        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Tài khoản ngân hàng chưa được mở hoặc eKYC chưa được phê duyệt!"));

        if (!account.getActive()) {
            throw new RuntimeException("Tài khoản ngân hàng của bạn đang bị khóa!");
        }

        // Ánh xạ dữ liệu sang DTO trả về cho Client
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .active(account.getActive())
                .build();
    }

    // Chuyển tiền nội bộ/liên ngân hàng (FR-07)
    // Annotation @Transactional bắt buộc để rollback nếu xảy ra bất kỳ lỗi gì ở giữa quá trình chuyển tiền
    @Override
    @Transactional
    public Transaction transfer(User user, TransferRequest request) {
        // 1. Tìm tài khoản nguồn (gửi tiền) của User hiện tại
        Account sourceAccount = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Bạn chưa được cấp tài khoản ngân hàng hoặc chưa eKYC!"));

        if (!sourceAccount.getActive()) {
            throw new RuntimeException("Tài khoản gửi của bạn đang bị khóa!");
        }

        // 2. Kiểm tra xem tài khoản nhận có trùng tài khoản gửi hay không
        if (sourceAccount.getAccountNumber().equals(request.getTargetAccountNumber())) {
            throw new RuntimeException("Không thể thực hiện chuyển tiền đến chính tài khoản này!");
        }

        // 3. Kiểm tra mã PIN giao dịch
        if (!passwordEncoder.matches(request.getTransactionPin(), sourceAccount.getTransactionPin())) {
            throw new RuntimeException("Mã PIN giao dịch không chính xác!");
        }

        // 4. Tìm tài khoản đích (nhận tiền)
        Account targetAccount = accountRepository.findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new RuntimeException("Tài khoản thụ hưởng không tồn tại trên hệ thống!"));

        if (!targetAccount.getActive()) {
            throw new RuntimeException("Tài khoản thụ hưởng hiện đang bị khóa!");
        }

        // 5. Áp dụng Khóa bi quan (Pessimistic Lock) để chống lỗi Double-spending
        // Để tránh Deadlock (nút thắt cổ chai), ta khóa các tài khoản theo thứ tự số tài khoản nhỏ hơn trước
        if (sourceAccount.getAccountNumber().compareTo(targetAccount.getAccountNumber()) < 0) {
            sourceAccount = accountRepository.findByAccountNumberWithLock(sourceAccount.getAccountNumber()).get();
            targetAccount = accountRepository.findByAccountNumberWithLock(targetAccount.getAccountNumber()).get();
        } else {
            targetAccount = accountRepository.findByAccountNumberWithLock(targetAccount.getAccountNumber()).get();
            sourceAccount = accountRepository.findByAccountNumberWithLock(sourceAccount.getAccountNumber()).get();
        }

        // 6. Kiểm tra xem số dư có đủ không
        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Số dư tài khoản không đủ để thực hiện chuyển khoản!");
        }

        // 7. Thực hiện trừ tiền tài khoản gửi, cộng tiền tài khoản nhận
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        targetAccount.setBalance(targetAccount.getBalance().add(request.getAmount()));

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        // 8. Tạo và ghi nhận lịch sử giao dịch thành công
        String txCode = "TX" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction transaction = Transaction.builder()
                .transactionCode(txCode)
                .amount(request.getAmount())
                .description(request.getDescription())
                .status("SUCCESS") // Ghi nhận thành công
                .fromAccount(sourceAccount)
                .toAccount(targetAccount)
                .build();

        return transactionRepository.save(transaction);
    }

    // Xem sao kê lịch sử giao dịch (FR-08)
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getStatement(User user, Pageable pageable) {
        // 1. Tìm tài khoản của người dùng
        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Bạn chưa mở tài khoản ngân hàng!"));

        // 2. Truy vấn danh sách giao dịch liên quan (chuyển đi HOẶC nhận về) từ Repository
        Page<Transaction> transactions = transactionRepository.findByAccountFromOrTo(account, pageable);

        // 3. Ánh xạ từ thực thể Transaction sang DTO và tự động gán nhãn loại giao dịch CREDIT/DEBIT (UC-06)
        return transactions.map(tx -> {
            String type = "DEBIT"; // Mặc định là trừ tiền (Chuyển đi)
            if (tx.getToAccount().getId().equals(account.getId())) {
                type = "CREDIT"; // Nếu tài khoản nhận trùng với tài khoản hiện tại -> Cộng tiền (Nhận về)
            }

            return TransactionResponseDto.builder()
                    .id(tx.getId())
                    .transactionCode(tx.getTransactionCode())
                    .amount(tx.getAmount())
                    .description(tx.getDescription())
                    .status(tx.getStatus())
                    .type(type)
                    .fromAccountNumber(tx.getFromAccount().getAccountNumber())
                    .toAccountNumber(tx.getToAccount().getAccountNumber())
                    .createdAt(tx.getCreatedAt())
                    .build();
        });
    }

    // Đổi mã PIN giao dịch (FR-10)
    @Override
    @Transactional
    public void changePin(User user, ChangePinRequest request) {
        // 1. Tìm tài khoản của người dùng
        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Bạn chưa mở tài khoản ngân hàng!"));

        // 2. Xác thực mã PIN cũ
        if (!passwordEncoder.matches(request.getOldPin(), account.getTransactionPin())) {
            throw new RuntimeException("Mã PIN cũ không chính xác!");
        }

        // 3. Cập nhật mã PIN mới băm mã hóa BCrypt
        account.setTransactionPin(passwordEncoder.encode(request.getNewPin()));
        accountRepository.save(account);
    }
}