package com.project_cuoimon.controller;

import com.project_cuoimon.dto.ApiResponse;
import com.project_cuoimon.dto.UserResponseDto;
import com.project_cuoimon.entity.KycProfile;
import com.project_cuoimon.entity.Status;
import com.project_cuoimon.entity.User;
import com.project_cuoimon.repository.UserRepository;
import com.project_cuoimon.security.UserDetailsImpl;
import com.project_cuoimon.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    // 1. API Khách hàng tải hồ sơ eKYC (FR-04 / Role CUSTOMER)
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/customer/kyc/upload")
    public ResponseEntity<ApiResponse<KycProfile>> uploadKyc(
            @AuthenticationPrincipal UserDetailsImpl userDetails, 
            @RequestParam("idNumber") String idNumber,
            @RequestParam("fullName") String fullName,
            @RequestParam("dob") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam("sex") String sex,
            @RequestParam("address") String address,
            @RequestParam("file") MultipartFile file) throws IOException {

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hiện tại!"));

        KycProfile profile = userService.uploadKyc(user, idNumber, fullName, dob, sex, address, file);

        ApiResponse<KycProfile> response = ApiResponse.<KycProfile>builder()
                .success(true)
                .message("Tải lên hồ sơ eKYC thành công! Vui lòng chờ nhân viên duyệt.")
                .data(profile)
                .build();
        return ResponseEntity.ok(response);
    }

    // 2. API Nhân viên phê duyệt eKYC (FR-09 / Role STAFF)
    @PreAuthorize("hasRole('STAFF')")
    @PostMapping("/staff/kyc/approve/{id}")
    public ResponseEntity<ApiResponse<KycProfile>> approveKyc(
            @PathVariable("id") Long kycProfileId,
            @RequestParam("status") Status status) {

        KycProfile profile = userService.approveKyc(kycProfileId, status);

        String msg = status == Status.CONFIRM ? "Đã phê duyệt eKYC và kích hoạt tài khoản thành công!" : "Đã từ chối eKYC của khách hàng.";

        ApiResponse<KycProfile> response = ApiResponse.<KycProfile>builder()
                .success(true)
                .message(msg)
                .data(profile)
                .build();
        return ResponseEntity.ok(response);
    }

    // 3. API Quản lý Người dùng: Lấy danh sách phân trang (FR-05 / Role ADMIN hoặc STAFF)
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponseDto>>> getAllUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UserResponseDto> users = userService.getAllUsers(pageable);

        ApiResponse<Page<UserResponseDto>> response = ApiResponse.<Page<UserResponseDto>>builder()
                .success(true)
                .message("Lấy danh sách người dùng thành công!")
                .data(users)
                .build();
        return ResponseEntity.ok(response);
    }

    // 4. API Khóa / Mở khóa tài khoản (Update - Role STAFF hoặc ADMIN)
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<User>> updateUserStatus(
            @PathVariable("id") Long userId,
            @RequestParam("isActive") Boolean isActive) {

        User updatedUser = userService.updateUserStatus(userId, isActive);
        String msg = isActive ? "Đã kích hoạt/Mở khóa tài khoản người dùng!" : "Đã khóa tài khoản người dùng!";

        ApiResponse<User> response = ApiResponse.<User>builder()
                .success(true)
                .message(msg)
                .data(updatedUser)
                .build();
        return ResponseEntity.ok(response);
    }

    // 5. API Xóa tài khoản người dùng (Delete - Chỉ duy nhất Role ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable("id") Long userId) {
        userService.deleteUser(userId);

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Đã xóa tài khoản người dùng khỏi hệ thống!")
                .data("Xóa thành công!")
                .build();
        return ResponseEntity.ok(response);
    }
}
