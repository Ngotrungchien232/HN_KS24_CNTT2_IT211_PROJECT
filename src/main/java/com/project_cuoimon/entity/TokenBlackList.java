package com.project_cuoimon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_blacklist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlackList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_token", nullable = false, unique = true, length = 500)
    private String accessToken;

    @Column(name = "expiry_at", nullable = false)
    private LocalDateTime expiryAt; // Thời điểm hết hạn của AccessToken

    @Column(name = "blacklisted_at", nullable = false)
    private LocalDateTime blacklistedAt; // Thời điểm đưa vào danh sách đen

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Biết được token bị blacklist này thuộc về user nào

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.blacklistedAt = LocalDateTime.now();
    }
}