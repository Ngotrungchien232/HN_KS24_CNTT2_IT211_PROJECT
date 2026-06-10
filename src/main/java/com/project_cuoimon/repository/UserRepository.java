package com.project_cuoimon.repository;

import com.project_cuoimon.entity.User;
import com.project_cuoimon.dto.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // Kỹ thuật JPQL Constructor Projection tối ưu hóa RAM (Yêu cầu UC-02)
    @Query("SELECT new com.project_cuoimon.dto.UserResponseDto(u.id, u.username, u.email, u.phoneNumber, u.isActive, u.isKyc, u.role.name, u.createdAt) FROM User u")
    Page<UserResponseDto> findAllUsersProjected(Pageable pageable);
    //Từ khóa new kết hợp đường dẫn DTO: Báo cho MySQL biết:
    // "Tôi chỉ muốn lấy đúng 8 cột dữ liệu này thôi, hãy nạp nó trực tiếp vào Constructor của lớp UserResponseDto và trả về luôn".
    // qua đó chúng ta có thể tối ưu ram một cách hiệu quả
}