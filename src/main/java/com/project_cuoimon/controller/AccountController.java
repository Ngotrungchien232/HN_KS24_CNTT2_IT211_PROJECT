package com.project_cuoimon.controller;

import com.project_cuoimon.dto.*;
import com.project_cuoimon.entity.Transaction;
import com.project_cuoimon.entity.User;
import com.project_cuoimon.repository.UserRepository;
import com.project_cuoimon.security.UserDetailsImpl;
import com.project_cuoimon.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserRepository userRepository;

    // 1. API Vấn tin số dư tài khoản (FR-06 / Role CUSTOMER)
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<AccountResponse>> getBalance(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Lấy thông tin User từ UserDetailsImpl đã được load trong quá trình xác thực
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng!"));

        AccountResponse accountResponse = accountService.getBalance(user);
        // Đóng gói dữ liệu vào ApiResponse để trả về cho Client
        ApiResponse<AccountResponse> response = ApiResponse.<AccountResponse>builder()
                .success(true)
                .message("Vấn tin số dư tài khoản thành công!")
                .data(accountResponse)
                .build();
        return ResponseEntity.ok(response);
    }

    // 2. API Chuyển tiền (Nội bộ / Liên ngân hàng - FR-07 / Role CUSTOMER)
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Transaction>> transfer(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody TransferRequest request) {
        // Lấy thông tin User từ UserDetailsImpl đã được load trong quá trình xác thực
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng!"));

        Transaction transaction = accountService.transfer(user, request);
    // Đóng gói dữ liệu vào ApiResponse để trả về cho Client
        ApiResponse<Transaction> response = ApiResponse.<Transaction>builder()
                .success(true)
                .message("Giao dịch chuyển khoản thành công!")
                .data(transaction)
                .build();
        return ResponseEntity.ok(response);
    }

    // 3. API Xem sao kê lịch sử giao dịch (FR-08 / Role CUSTOMER)
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/statement")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getStatement(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng!"));

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionResponseDto> statement = accountService.getStatement(user, pageable);

        ApiResponse<Page<TransactionResponseDto>> response = ApiResponse.<Page<TransactionResponseDto>>builder()
                .success(true)
                .message("Tải sao kê lịch sử giao dịch thành công!")
                .data(statement)
                .build();
        return ResponseEntity.ok(response);
    }

    // 4. API Đổi mã PIN giao dịch (FR-10 / Role CUSTOMER)
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/change-pin")
    public ResponseEntity<ApiResponse<String>> changePin(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChangePinRequest request) {

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng!"));

        accountService.changePin(user, request);

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Đổi mã PIN giao dịch thành công!")
                .data("Đổi PIN thành công!")
                .build();
        return ResponseEntity.ok(response);
    }
}
