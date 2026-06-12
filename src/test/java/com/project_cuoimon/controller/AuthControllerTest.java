package com.project_cuoimon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_cuoimon.dto.ForgotPasswordRequest;
import com.project_cuoimon.dto.JwtResponse;
import com.project_cuoimon.dto.LoginRequest;
import com.project_cuoimon.dto.RegisterRequest;
import com.project_cuoimon.entity.Role;
import com.project_cuoimon.entity.User;
import com.project_cuoimon.repository.RefreshTokenRepository;
import com.project_cuoimon.repository.TokenBlackListRepository;
import com.project_cuoimon.security.JwtUtils;
import com.project_cuoimon.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenBlackListRepository tokenBlackListRepository;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    // Test Case 1: Đăng ký khoản khách hàng thành công
    @Test
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("customer1");
        request.setPassword("password123");
        request.setEmail("customer1@gmail.com");
        request.setPhoneNumber("0987654321");

        User user = User.builder()
                .id(1L)
                .username("customer1")
                .email("customer1@gmail.com")
                .build();

        when(userService.registerCustomer(any(RegisterRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng ký tài khoản khách hàng thành công!"))
                .andExpect(jsonPath("$.data.username").value("customer1"));
        
        verify(userService, times(1)).registerCustomer(any(RegisterRequest.class));
    }

    // Test Case 2: Đăng nhập thành công trả về JwtResponse
    @Test
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("customer1");
        request.setPassword("password123");

        Authentication authentication = mock(Authentication.class);
        com.project_cuoimon.security.UserDetailsImpl userDetails = new com.project_cuoimon.security.UserDetailsImpl(
                1L, "customer1", "customer1@gmail.com", "encoded_password", true, new ArrayList<>()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("mock_access_token");
        when(authentication.getPrincipal()).thenReturn(userDetails);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng nhập thành công!"))
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"))
                .andExpect(jsonPath("$.data.username").value("customer1"));
    }

    // Test Case 3: Quên mật khẩu thành công
    @Test
    void forgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsername("customer1");
        request.setEmail("customer1@gmail.com");

        when(userService.forgotPassword(any(ForgotPasswordRequest.class))).thenReturn("Rikkei@123");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Rikkei@123"));
    }
}
