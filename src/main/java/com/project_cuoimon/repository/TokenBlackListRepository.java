package com.project_cuoimon.repository;

import com.project_cuoimon.entity.TokenBlackList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenBlackListRepository extends JpaRepository<TokenBlackList, Long> {
    // Kiểm tra xem access token đã tồn tại trong blacklist hay chưa
    boolean existsByAccessToken(String accessToken);
}