package com.project_cuoimon.service;

import com.project_cuoimon.dto.RegisterRequest;
import com.project_cuoimon.entity.*;
import com.project_cuoimon.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private KycProfileRepository kycProfileRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest registerRequest;
    private Role customerRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("customer1");
        registerRequest.setPassword("password123");
        registerRequest.setEmail("customer1@gmail.com");
        registerRequest.setPhoneNumber("0987654321");

        customerRole = Role.builder()
                .id(3L)
                .name("ROLE_CUSTOMER")
                .description("Customer Role")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("customer1")
                .password("encoded_password")
                .email("customer1@gmail.com")
                .phoneNumber("0987654321")
                .role(customerRole)
                .isKyc(false)
                .build();
    }

    // Test Case 1: Đăng ký người dùng mới thành công
    @Test
    void registerCustomer_Success() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User savedUser = userService.registerCustomer(registerRequest);

        assertNotNull(savedUser);
        assertEquals("customer1", savedUser.getUsername());
        assertEquals("ROLE_CUSTOMER", savedUser.getRole().getName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    // Test Case 2: Đăng ký thất bại do tên đăng nhập đã tồn tại
    @Test
    void registerCustomer_UsernameExists_ThrowsException() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerCustomer(registerRequest);
        });

        assertEquals("Tên đăng nhập đã tồn tại trong hệ thống!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // Test Case 3: Tải ảnh eKYC thất bại do trùng số CCCD
    @Test
    void uploadKyc_DuplicateIdNumber_ThrowsException() throws IOException {
        String idNumber = "001202000001";
        MultipartFile mockFile = mock(MultipartFile.class);

        when(kycProfileRepository.existsByIdNumber(idNumber)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.uploadKyc(testUser, idNumber, "Nguyen Van A", LocalDate.of(2000, 1, 1),
                    "Nam", "Ha Noi", mockFile);
        });

        assertEquals("Số định danh CCCD/Passport này đã được đăng ký bởi tài khoản khác!", exception.getMessage());
        verify(cloudinaryService, never()).uploadFile(any());
    }

    // Test Case 4: Phê duyệt KYC thành công và tự động tạo tài khoản ngân hàng
    @Test
    void approveKyc_Confirm_Success() {
        KycProfile pendingProfile = KycProfile.builder()
                .id(1L)
                .idNumber("001202000001")
                .fullName("Nguyen Van A")
                .user(testUser)
                .status(Status.PENDING)
                .build();

        when(kycProfileRepository.findById(1L)).thenReturn(Optional.of(pendingProfile));
        when(accountRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encoded_pin");
        when(kycProfileRepository.save(any(KycProfile.class))).thenReturn(pendingProfile);

        KycProfile approvedProfile = userService.approveKyc(1L, Status.CONFIRM);

        assertNotNull(approvedProfile);
        assertEquals(Status.CONFIRM, approvedProfile.getStatus());
        assertTrue(testUser.getIsKyc());
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(kycProfileRepository, times(1)).save(any(KycProfile.class));
    }
}
