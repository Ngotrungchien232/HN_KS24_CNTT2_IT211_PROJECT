package com.project_cuoimon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_cuoimon.dto.RegisterRequest;
import com.project_cuoimon.entity.Role;
import com.project_cuoimon.repository.RoleRepository;
import com.project_cuoimon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectCuoiMonApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void contextLoads() {
        // Kiểm tra xem ứng dụng có khởi động context thành công hay không
    }

    @Test
    void testRegisterCustomer_Success() throws Exception {
        // Tạo vai trò CUSTOMER nếu chưa tồn tại
        Role role = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name("ROLE_CUSTOMER")
                                .description("Customer Role")
                                .build()
                ));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("test_integration_user");
        request.setPassword("password123");
        request.setEmail("test_integration@rikkeibank.com");
        request.setPhoneNumber("0111222333");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng ký tài khoản khách hàng thành công!"));
    }

    @Test
    void testRegisterCustomer_DuplicateUsername_ThrowsConflict() throws Exception {
        Role role = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name("ROLE_CUSTOMER")
                                .description("Customer Role")
                                .build()
                ));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("test_duplicate_user");
        request.setPassword("password123");
        request.setEmail("test_dup1@rikkeibank.com");
        request.setPhoneNumber("0222333444");

        // Đăng ký lần đầu thành công
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Đăng ký lần hai trùng username phải trả về lỗi 409 Conflict từ GlobalExceptionHandler
        RegisterRequest request2 = new RegisterRequest();
        request2.setUsername("test_duplicate_user");
        request2.setPassword("password456");
        request2.setEmail("test_dup2@rikkeibank.com");
        request2.setPhoneNumber("0222333445");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập đã tồn tại trong hệ thống!"));
    }
}
