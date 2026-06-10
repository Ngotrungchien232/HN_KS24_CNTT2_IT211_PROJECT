package com.project_cuoimon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
// packet dto để nhận dữ liệu từ client gửi lên, sau đó sẽ được chuyển thành entity để lưu vào database

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}