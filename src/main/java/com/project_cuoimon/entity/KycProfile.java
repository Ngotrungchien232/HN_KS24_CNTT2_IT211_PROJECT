package com.project_cuoimon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_number", nullable = false, unique = true, length = 50)
    private String idNumber; // Số CCCD hoặc Passport

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "dob")
    private LocalDate dob; // Ngày sinh

    @Column(length = 10)
    private String sex; // Giới tính

    @Column(length = 255)
    private String address;

    @Column(name = "id_card_front_url", length = 255)
    private String idCardFrontUrl; // URL ảnh CCCD lưu trên Cloudinary

    @Enumerated(EnumType.STRING) // Lưu Enum dưới dạng chuỗi (PENDING, CONFIRM...)
    @Column(length = 20, nullable = false)
    private Status status;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt; // Ngày duyệt hồ sơ

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Thiết lập mối quan hệ Một-Một với User
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = Status.PENDING; // Mặc định khi tạo mới là chờ duyệt
        }
    }
}