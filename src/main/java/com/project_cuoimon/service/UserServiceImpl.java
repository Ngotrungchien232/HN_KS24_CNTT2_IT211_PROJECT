package com.project_cuoimon.service;

import com.project_cuoimon.entity.*;
import com.project_cuoimon.dto.RegisterRequest;
import com.project_cuoimon.dto.UserResponseDto;
import com.project_cuoimon.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private KycProfileRepository kycProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.project_cuoimon.service.CloudinaryService cloudinaryService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User registerCustomer(RegisterRequest request) {
        // 1. Kiểm tra xem tên đăng nhập hoặc email đã được đăng ký chưa
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại trong hệ thống!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        // 2. Tìm Role mặc định là ROLE_CUSTOMER
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò khách hàng (ROLE_CUSTOMER) trên hệ thống!"));

        // 3. Tạo đối tượng User và lưu vào CSDL
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword())) // Mã hóa BCrypt mật khẩu
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .role(customerRole)
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public KycProfile uploadKyc(User user, String idNumber, String fullName, LocalDate dob,
                                String sex, String address, MultipartFile file) throws IOException {

        // 1. Kiểm tra xem User này đã từng làm KYC hoặc đang chờ duyệt chưa
        if (user.getIsKyc()) {
            throw new RuntimeException("Tài khoản của bạn đã được định danh eKYC thành công trước đó!");
        }

        kycProfileRepository.findByUser(user).ifPresent(profile -> {
            if (profile.getStatus() == Status.PENDING) {
                throw new RuntimeException("Hồ sơ định danh eKYC của bạn đang trong trạng thái chờ duyệt!");
            }
        });

        // 2. Kiểm tra xem số CCCD đã bị trùng lặp trong hệ thống chưa
        if (kycProfileRepository.existsByIdNumber(idNumber)) {
            throw new RuntimeException("Số định danh CCCD/Passport này đã được đăng ký bởi tài khoản khác!");
        }

        // 3. Gọi CloudinaryService để đẩy ảnh lên đám mây và lấy URL
        String imageUrl = cloudinaryService.uploadFile(file);

        // 4. Tìm kiếm hồ sơ cũ hoặc tạo hồ sơ eKYC mới
        KycProfile kycProfile = kycProfileRepository.findByUser(user)
                .orElse(new KycProfile());

        kycProfile.setUser(user);
        kycProfile.setIdNumber(idNumber);
        kycProfile.setFullName(fullName);
        kycProfile.setDob(dob);
        kycProfile.setSex(sex);
        kycProfile.setAddress(address);
        kycProfile.setIdCardFrontUrl(imageUrl);
        kycProfile.setStatus(Status.PENDING); // Đặt trạng thái chờ duyệt

        return kycProfileRepository.save(kycProfile);
    }

    @Override
    @Transactional
    public KycProfile approveKyc(Long kycProfileId, Status status) {
        // 1. Tìm hồ sơ eKYC theo ID
        KycProfile profile = kycProfileRepository.findById(kycProfileId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ định danh có ID: " + kycProfileId));

        if (profile.getStatus() != Status.PENDING) {
            throw new RuntimeException("Hồ sơ này đã được xử lý từ trước!");
        }

        User user = profile.getUser();

        // 2. Cập nhật trạng thái duyệt hồ sơ
        profile.setStatus(status);
        profile.setVerifiedAt(LocalDateTime.now());

        if (status == Status.CONFIRM) {
            // Đánh dấu người dùng đã hoàn thành eKYC
            user.setIsKyc(true);
            userRepository.save(user);

            // TỰ ĐỘNG MỞ TÀI KHOẢN NGÂN HÀNG CHO KHÁCH HÀNG
            if (accountRepository.findByUser(user).isEmpty()) {
                String accountNumber = generateUniqueAccountNumber();
                Account account = Account.builder()
                        .accountNumber(accountNumber)
                        .balance(BigDecimal.valueOf(50000)) // Tặng 50.000 VND làm số dư tối thiểu ban đầu
                        .currency("VND")
                        .transactionPin(passwordEncoder.encode("123456")) // Mã PIN mặc định là 123456 (được mã hóa)
                        .active(true)
                        .user(user)
                        .build();
                accountRepository.save(account);
            }
            // giải thích về vấn đề tự động mở tài khoản ngân hàng cho khách hàng sau khi quản trị duyệt
            // Nhân viên Staff gửi yêu cầu phê duyệt với trạng thái CONFIRM.
            //Hệ thống cập nhật thuộc tính isKyc = true cho User để đánh dấu tài khoản này đã được xác thực danh tính chính chủ.
            //Hệ thống tự động sinh số tài khoản ngẫu nhiên gồm 11 chữ số (bắt đầu bằng đầu số ngân hàng 999...) và kiểm tra xem có bị trùng trong database không.
            //Khởi tạo thực thể Account mới với số tài khoản vừa sinh, cộng sẵn 50,000 VND làm số dư tối thiểu ban đầu, và mã hóa mã PIN mặc định 123456 trước khi lưu vào CSDL.

        }

        return kycProfileRepository.save(profile);
    }

    @Override
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        // Truy vấn trực tiếp DTO thông qua JPQL Constructor Projection tối ưu RAM
        return userRepository.findAllUsersProjected(pageable);
    }

    // Hàm tạo số tài khoản ngẫu nhiên gồm 11 chữ số đảm bảo không trùng lặp
    private String generateUniqueAccountNumber() {
        Random random = new Random();
        String accountNumber;
        do {
            // Ví dụ số tài khoản bắt đầu bằng "999" tiếp theo là 8 chữ số ngẫu nhiên
            long number = 99900000000L + random.nextInt(100000000);
            accountNumber = String.valueOf(number);
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    @Override
    @Transactional
    public User updateUserStatus(Long userId, Boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng có ID: " + userId));

        user.setIsActive(isActive);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng có ID: " + userId));

        // Xóa các thực thể liên quan trước để tránh lỗi khóa ngoại (Foreign Key Constraints)
        refreshTokenRepository.deleteByUser(user);
        kycProfileRepository.findByUser(user).ifPresent(kycProfileRepository::delete);
        accountRepository.findByUser(user).ifPresent(accountRepository::delete);

        userRepository.delete(user);
    }

    @Override
    @Transactional
    public String forgotPassword(com.project_cuoimon.dto.ForgotPasswordRequest request) {
        // 1. Tìm kiếm User theo username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập không tồn tại trên hệ thống!"));

        // 2. Kiểm tra xem email có khớp không
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new RuntimeException("Email không trùng khớp với thông tin đã đăng ký!");
        }

        // 3. Tạo mật khẩu tạm thời đơn giản dễ nhớ theo yêu cầu của học viên (ví dụ: "Rikkei@123")
        String tempPassword = java.util.UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(tempPassword)); // Mã hóa BCrypt mật khẩu mới
        userRepository.save(user);

        return tempPassword;
    }
}