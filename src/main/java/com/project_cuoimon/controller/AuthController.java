package com.project_cuoimon.controller;

import com.project_cuoimon.dto.*;
import com.project_cuoimon.entity.RefreshToken;
import com.project_cuoimon.entity.TokenBlackList;
import com.project_cuoimon.entity.User;
import com.project_cuoimon.repository.RefreshTokenRepository;
import com.project_cuoimon.repository.TokenBlackListRepository;
import com.project_cuoimon.security.JwtUtils;
import com.project_cuoimon.security.UserDetailsImpl;
import com.project_cuoimon.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils; // Utility để tạo và xác thực JWT 

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenBlackListRepository tokenBlackListRepository;

    // 1. API Đăng ký tài khoản (FR-04 / Public)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerCustomer(request);
        
        ApiResponse<User> response = ApiResponse.<User>builder()
                .success(true)
                .message("Đăng ký tài khoản khách hàng thành công!")
                .data(user)
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 2. API Đăng nhập (FR-01 / Public)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        User user = User.builder().id(userPrincipal.getId()).build();

        // Xoay vòng Refresh Token (24 giờ)
        refreshTokenRepository.deleteByUser(user); // Xóa Refresh Token cũ nếu có
        String tokenStr = UUID.randomUUID().toString();
        
        Instant expiryDate = Instant.now().plusMillis(86400000); 
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenStr)
                .expiryDate(expiryDate)
                .revoked(false)
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken);

        JwtResponse jwtResponse = JwtResponse.builder()
                .accessToken(jwt)
                .refreshToken(tokenStr)
                .id(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .email(userPrincipal.getEmail())
                .roles(roles)
                .build();

        ApiResponse<JwtResponse> response = ApiResponse.<JwtResponse>builder()
                .success(true)
                .message("Đăng nhập thành công!")
                .data(jwtResponse)
                .build();
        return ResponseEntity.ok(response);
    }

    // 3. API Xoay vòng Token (FR-02 / Public)
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token không tồn tại trong hệ thống!"));

        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh Token đã hết hạn! Vui lòng thực hiện đăng nhập lại.");
        }

        String newAccessToken = jwtUtils.generateTokenFromUsername(token.getUser().getUsername());

        TokenRefreshResponse refreshResponse = TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token.getToken())
                .build();

        ApiResponse<TokenRefreshResponse> response = ApiResponse.<TokenRefreshResponse>builder()
                .success(true)
                .message("Lấy Access Token mới thành công!")
                .data(refreshResponse)
                .build();
        return ResponseEntity.ok(response);
    }

    // 4. API Đăng xuất (FR-03 / Authenticated)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            
            TokenBlackList blacklistToken = TokenBlackList.builder()
                    .accessToken(jwt)
                    .expiryAt(LocalDateTime.now().plusMinutes(5)) 
                    .build();
            tokenBlackListRepository.save(blacklistToken);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
                User user = User.builder().id(userPrincipal.getId()).build();
                refreshTokenRepository.deleteByUser(user);
            }
        }

        SecurityContextHolder.clearContext();

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Đăng xuất thành công và vô hiệu hóa Token!")
                .data("Đăng xuất thành công!")
                .build();
        return ResponseEntity.ok(response);
    }

    // 5. API Quên mật khẩu - Cấp mật khẩu tạm thời (FR-10 / Public)
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String tempPassword = userService.forgotPassword(request);

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Khôi phục mật khẩu thành công! Mật khẩu tạm thời mới của bạn là: " + tempPassword)
                .data(tempPassword)
                .build();
        return ResponseEntity.ok(response);
    }
}
