package com.project_cuoimon.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private Boolean isActive;
    private Boolean isKyc;
    private String roleName;
    private LocalDateTime createdAt;

    // Constructor bắt buộc phải khớp với truy vấn JPQL sau này ở tầng Repository
    public UserResponseDto(Long id, String username, String email, String phoneNumber,
                           Boolean isActive, Boolean isKyc, String roleName, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.isActive = isActive;
        this.isKyc = isKyc;
        this.roleName = roleName;
        this.createdAt = createdAt;
    }
}