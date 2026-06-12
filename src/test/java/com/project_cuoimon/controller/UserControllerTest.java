package com.project_cuoimon.controller;

import com.project_cuoimon.entity.KycProfile;
import com.project_cuoimon.entity.Status;
import com.project_cuoimon.entity.User;
import com.project_cuoimon.repository.UserRepository;
import com.project_cuoimon.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    // Test Case 4: Phê duyệt KYC thành công (trả về trạng thái CONFIRM)
    @Test
    void approveKyc_Success() throws Exception {
        KycProfile approvedProfile = KycProfile.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .status(Status.CONFIRM)
                .build();

        when(userService.approveKyc(1L, Status.CONFIRM)).thenReturn(approvedProfile);

        mockMvc.perform(post("/api/v1/staff/kyc/approve/1")
                .param("status", "CONFIRM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã phê duyệt eKYC và kích hoạt tài khoản thành công!"))
                .andExpect(jsonPath("$.data.status").value("CONFIRM"));

        verify(userService, times(1)).approveKyc(1L, Status.CONFIRM);
    }

    // Test Case 5: Khóa/Mở khóa tài khoản thành công
    @Test
    void updateUserStatus_Success() throws Exception {
        User updatedUser = User.builder()
                .id(1L)
                .username("customer1")
                .isActive(false)
                .build();

        when(userService.updateUserStatus(1L, false)).thenReturn(updatedUser);

        mockMvc.perform(put("/api/v1/users/1/status")
                .param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã khóa tài khoản người dùng!"))
                .andExpect(jsonPath("$.data.isActive").value(false));

        verify(userService, times(1)).updateUserStatus(1L, false);
    }
}
