package com.project_cuoimon.controller;

import com.project_cuoimon.dto.AccountResponse;
import com.project_cuoimon.dto.ApiResponse;
import com.project_cuoimon.entity.User;
import com.project_cuoimon.repository.UserRepository;
import com.project_cuoimon.security.UserDetailsImpl;
import com.project_cuoimon.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng!"));

        AccountResponse accountResponse = accountService.getBalance(user);

        ApiResponse<AccountResponse> response = ApiResponse.<AccountResponse>builder()
                .success(true)
                .message("Vấn tin số dư tài khoản thành công!")
                .data(accountResponse)
                .build();
        return ResponseEntity.ok(response);
    }
}
