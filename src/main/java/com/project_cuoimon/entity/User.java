package com.project_cuoimon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255) // Lưu pass BCrypt đã mã hóa
    private String password;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(length = 100)
    private String email;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_kyc", nullable = false)
    private Boolean isKyc;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Thiết lập mối quan hệ Nhiều User - Một Role
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // Tự động thiết lập giá trị mặc định trước khi lưu vào database
    @PrePersist
    // Khi một User mới được tạo, sẽ tự động thiết lập createdAt là thời điểm hiện tại, isActive mặc định là true và isKyc mặc định là false
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.isKyc == null) {
            this.isKyc = false;
        }
    }


}