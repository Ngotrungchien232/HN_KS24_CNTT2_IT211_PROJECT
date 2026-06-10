package com.project_cuoimon.service;

import com.project_cuoimon.entity.KycProfile;
import com.project_cuoimon.entity.Status;
import com.project_cuoimon.entity.User;
import com.project_cuoimon.dto.RegisterRequest;
import com.project_cuoimon.dto.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

public interface UserService {
    // Đăng ký tài khoản người dùng mới (Public)
    User registerCustomer(RegisterRequest request);

    // Tải lên hồ sơ eKYC kèm ảnh chụp CCCD gửi lên Cloudinary (Customer)
    KycProfile uploadKyc(User user, String idNumber, String fullName, LocalDate dob,
                         String sex, String address, MultipartFile file) throws IOException;

    // Phê duyệt hồ sơ định danh eKYC và mở tài khoản ngân hàng (Staff)
    KycProfile approveKyc(Long kycProfileId, Status status);

    // Lấy danh sách người dùng phân trang dùng JPQL Constructor Projection (Admin/Staff)
    Page<UserResponseDto> getAllUsers(Pageable pageable);
}