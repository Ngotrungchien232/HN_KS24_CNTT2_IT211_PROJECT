# CẨM NANG GIẢI THÍCH CHI TIẾT CẤU TRÚC VÀ LOGIC MÃ NGUỒN RIKKEI BANK

Chào em! Tài liệu này được biên soạn nhằm giải thích cặn kẽ vai trò của từng gói thư mục (**package**) và logic hoạt động của từng tệp (**file**) mã nguồn trong toàn bộ dự án. Đây sẽ là "bí kíp" giúp em hiểu sâu sắc dự án và tự tin vượt qua mọi câu hỏi của hội đồng chấm đồ án.

---

## I. TỔNG QUAN VỀ KIẾN TRÚC 3 LỚP (3-LAYER ARCHITECTURE)
Dự án của em được phân tách theo mô hình kiến trúc 3 lớp tiêu chuẩn công nghiệp:
1.  **Presentation Layer (`controller`):** Tiếp nhận yêu cầu HTTP từ client (như Bruno, Postman, Frontend Web/Mobile), kiểm tra dữ liệu thô sơ và trả lại JSON.
2.  **Business Logic Layer (`service`):** Trực tiếp giải quyết các thuật toán, logic nghiệp vụ, tính toán số dư ngân hàng và xử lý nghiệp vụ chính.
3.  **Data Access Layer (`repository` & `entity`):** Tương tác vật lý với Cơ sở dữ liệu MySQL thông qua Spring Data JPA và Hibernate để lưu giữ thông tin lâu dài.

---

## II. GIẢI THÍCH CHI TIẾT TỪNG PACKAGE VÀ FILE LOGIC

### 1. Gói cấu hình hệ thống: `com.project_cuoimon.config`
Chứa các cài đặt môi trường ban đầu, thiết lập bảo mật và kết nối dịch vụ bên thứ ba.

*   **[SecurityConfig.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/config/SecurityConfig.java):**
    *   *Tác dụng:* Cấu hình chuỗi bảo mật chính của Spring Security.
    *   *Logic:*
        *   Tạo đối tượng `BCryptPasswordEncoder` để mã hóa mật khẩu người dùng trước khi lưu.
        *   Thiết lập phân quyền URL: Cho phép `/api/auth/**` và `/error` truy cập tự do không cần Token (`permitAll()`), phân quyền rõ ràng vai trò `ADMIN`, `STAFF`, `CUSTOMER` cho các đường dẫn cụ thể, và bắt buộc tất cả các request khác phải đăng nhập (`anyRequest().authenticated()`).
        *   Tích hợp bộ lọc Token `AuthTokenFilter` vào trước chuỗi xử lý bảo mật.
*   **[CloudinaryConfig.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/config/CloudinaryConfig.java):**
    *   *Tác dụng:* Cấu hình kết nối tới Cloudinary (dịch vụ lưu trữ hình ảnh đám mây).
    *   *Logic:* Nạp tài khoản Cloudinary (`cloud_name`, `api_key`, `api_secret`) từ file cài đặt và khởi tạo đối tượng `Cloudinary` dưới dạng Spring Bean để toàn hệ thống sử dụng khi tải ảnh CCCD/Passport lên.
*   **[DatabaseSeeder.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/config/DatabaseSeeder.java):**
    *   *Tác dụng:* Tự động tạo dữ liệu mẫu khi khởi chạy ứng dụng lần đầu.
    *   *Logic:* Lắng nghe sự kiện ứng dụng sẵn sàng (`ApplicationReadyEvent`), tự động kiểm tra xem bảng `roles` trong MySQL đã chứa 3 quyền cơ bản (`ROLE_ADMIN`, `ROLE_STAFF`, `ROLE_CUSTOMER`) chưa. Nếu chưa có, nó sẽ chèn dữ liệu tự động vào để tránh lỗi truy vấn phân quyền sau này.

---

### 2. Gói bảo mật và kiểm soát truy cập: `com.project_cuoimon.security`
Bộ phận thực thi xác thực JWT và phân quyền người dùng trong Spring Security.

*   **[AuthTokenFilter.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/security/AuthTokenFilter.java):**
    *   *Tác dụng:* Bộ lọc chặn mọi yêu cầu HTTP để kiểm tra tính hợp lệ của token bảo mật.
    *   *Logic:* Trích xuất chuỗi JWT trong Header `Authorization: Bearer <token>`. Nó sẽ kiểm tra token xem có nằm trong bảng **Blacklist** (danh sách đen do đã nhấn Logout) hay không. Nếu bị blacklist, nó trực tiếp trả về lỗi `401 Unauthorized` ngay lập tức. Nếu hợp lệ, nó lấy tên người dùng, nạp thông tin quyền hạn vào ngữ cảnh bảo mật (`SecurityContext`) để cho phép request đi tiếp.
*   **[JwtUtils.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/security/JwtUtils.java):**
    *   *Tác dụng:* Cung cấp các hàm tiện ích tạo và giải mã JWT Token.
    *   *Logic:*
        *   `generateJwtToken`: Tạo ra AccessToken (có hiệu lực 5 phút) chứa username và thời gian hết hạn, được ký số bằng thuật toán HMAC256.
        *   `getUserNameFromJwtToken`: Giải mã token để đọc ra tên người dùng.
        *   `validateJwtToken`: Kiểm tra chữ ký số của token xem có bị giả mạo hay đã hết hạn hay chưa.
*   **[UserDetailsImpl.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/security/UserDetailsImpl.java):**
    *   *Tác dụng:* Lớp bọc trung gian chuyển đổi đối tượng `User` của dự án thành định dạng `UserDetails` mà Spring Security hiểu được.
    *   *Logic:* Chứa thông tin về định danh (ID, username, email, mật khẩu đã mã hóa) và danh sách các quyền hạn (`GrantedAuthority`). Đặc biệt, phương thức `isEnabled()` được liên kết trực tiếp với trạng thái kích hoạt `isActive` của User trong database để ngăn chặn tài khoản bị khóa đăng nhập.
*   **[UserDetailsServiceImpl.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/security/UserDetailsServiceImpl.java):**
    *   *Tác dụng:* Nạp dữ liệu tài khoản từ Database dựa trên tên đăng nhập.
    *   *Logic:* Tìm kiếm User từ `UserRepository` bằng `username`. Nếu không tìm thấy, ném ra lỗi `UsernameNotFoundException`. Nếu tìm thấy, khởi tạo và trả về đối tượng `UserDetailsImpl`.
*   **[AuthEntryPointJwt.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/security/AuthEntryPointJwt.java):**
    *   *Tác dụng:* Xử lý khi có lỗi xảy ra ở bước xác thực (gửi yêu cầu không gửi kèm token hoặc token sai).
    *   *Logic:* Trực tiếp ghi mã lỗi HTTP `401 Unauthorized` và trả về JSON chuẩn chứa ngày giờ, mã lỗi, và thông báo lỗi chi tiết thay vì hiển thị trang lỗi HTML mặc định của Tomcat.

---

### 3. Gói thực thể mô hình hóa CSDL: `com.project_cuoimon.entity`
Chứa các lớp Java (Entities) định nghĩa cấu trúc bảng dữ liệu vật lý trong MySQL.

*   **[User.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/entity/User.java):** Đại diện cho bảng `users`. Chứa thông tin tài khoản đăng nhập, email, số điện thoại, trạng thái eKYC (`isKyc`), trạng thái kích hoạt (`isActive`), và liên kết nhiều-một (`@ManyToOne`) với bảng `Role`.
*   **[Role.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/entity/Role.java):** Đại diện cho bảng `roles`. Chứa thông tin tên vai trò (ví dụ: `ROLE_CUSTOMER`, `ROLE_STAFF`, `ROLE_ADMIN`).
*   **[KycProfile.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/entity/KycProfile.java):** Đại diện cho bảng `kyc_profiles` (hồ sơ định danh). Lưu trữ thông tin cá nhân của khách hàng như CCCD (`idNumber`), họ tên, giới tính, địa chỉ, ảnh mặt trước CCCD và trạng thái duyệt (`Status`).
*   **[Status.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/entity/Status.java):** Enum chứa 3 trạng thái eKYC hợp lệ duy nhất: `PENDING` (chờ phê duyệt), `CONFIRM` (đã duyệt thành công), và `REJECT` (từ chối phê duyệt).
*   **[RefreshToken.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/entity/RefreshToken.java):** Đại diện cho bảng `refresh_tokens`. Lưu trữ mã Token xoay vòng (hạn 24 giờ) kết nối với thực thể `User` để cấp lại AccessToken mới khi hết hạn.
*   **[TokenBlackList.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/entity/TokenBlackList.java):** Đại diện cho bảng `token_blacklist`. Chứa các AccessToken đã bị vô hiệu hóa khi người dùng bấm Đăng xuất.
*   **[Account.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/entity/Account.java):** Đại diện cho bảng `accounts` (Tài khoản ngân hàng của khách hàng). Chứa số tài khoản thanh toán gồm 11 chữ số, số dư (`balance`), loại tiền tệ, mã PIN giao dịch đã mã hóa, trạng thái hoạt động và liên kết với chủ tài khoản (`User`).
*   **[Transaction.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/entity/Transaction.java):** Đại diện cho bảng `transactions` (Lịch sử giao dịch). Lưu giữ thông tin số tiền chuyển, lời nhắn, thời gian thực hiện giao dịch và liên kết tới tài khoản gửi (`senderAccount`), tài khoản nhận (`receiverAccount`).

---

### 4. Gói giao tiếp Cơ sở dữ liệu: `com.project_cuoimon.repository`
Cung cấp các giao diện (Interfaces) thực hiện các câu lệnh truy vấn dữ liệu thông qua Spring Data JPA.

*   **[UserRepository.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/repository/UserRepository.java):**
    *   *Logic:* Cung cấp các câu lệnh kiểm tra tồn tại của `username` hoặc `email` trong database. Chứa phương thức đặc biệt `findAllUsersProjected` sử dụng kỹ thuật **JPQL Constructor Projection** để nạp trực tiếp danh sách thông tin người dùng rút gọn vào `UserResponseDto` mà không tải toàn bộ đối tượng User nặng nề lên RAM máy chủ.
*   **[AccountRepository.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/repository/AccountRepository.java):**
    *   *Logic:* Chứa câu lệnh truy vấn tài khoản ngân hàng của người dùng. Đặc biệt có phương thức `findByAccountNumber` được cấu hình với `@Lock(LockModeType.PESSIMISTIC_WRITE)` triển khai cơ chế **Khóa bi quan**. Khi gọi, dòng chứa tài khoản ngân hàng trong database MySQL sẽ bị khóa cứng để tránh tình trạng chi tiêu kép (nhiều giao dịch diễn ra cùng lúc trên một tài khoản).
*   **[TransactionRepository.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/repository/TransactionRepository.java):**
    *   *Logic:* Tìm kiếm lịch sử giao dịch. Chứa câu lệnh truy vấn JPQL dùng toán tử `OR` để tìm kiếm toàn bộ giao dịch mà tài khoản của người dùng đóng vai trò là tài khoản gửi HOẶC tài khoản nhận, đồng thời hỗ trợ phân trang và sắp xếp giảm dần theo thời gian.
*   **[KycProfileRepository.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/repository/KycProfileRepository.java) / [RefreshTokenRepository.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/repository/RefreshTokenRepository.java) / [TokenBlackListRepository.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/repository/TokenBlackListRepository.java):**
    *   *Logic:* Cung cấp các câu lệnh thao tác kiểm tra cơ bản liên quan đến hồ sơ KYC của người dùng, làm mới refresh token và kiểm tra token trong danh sách đen.

---

### 5. Gói đóng gói và truyền tải dữ liệu: `com.project_cuoimon.dto`
Chứa các đối tượng chỉ mang dữ liệu (Data Transfer Objects), bảo mật cấu trúc của thực thể chính.

*   **[ApiResponse.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/dto/ApiResponse.java):** Phong bì đóng gói phản hồi chuẩn của API bao gồm trạng thái thành công (`success`), thông báo (`message`) và dữ liệu trả về (`data`).
*   **[RegisterRequest.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/dto/RegisterRequest.java) & [LoginRequest.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/dto/LoginRequest.java):** Chứa thông tin đăng ký (email, phone, mật khẩu thô) và đăng nhập gửi lên từ client. Có áp dụng chú thích kiểm hợp như `@NotBlank`, `@Email`, `@Size` để kiểm tra dữ liệu đầu vào.
*   **[JwtResponse.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/dto/JwtResponse.java):** Gói dữ liệu trả về khi đăng nhập thành công bao gồm AccessToken, RefreshToken và thông tin vai trò người dùng để hiển thị trên client.
*   **[ForgotPasswordRequest.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/dto/ForgotPasswordRequest.java):** Dữ liệu yêu cầu cấp lại mật khẩu tạm thời gồm username và email đăng ký.
*   **[TransferRequest.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/dto/TransferRequest.java):** Đóng gói thông tin yêu cầu chuyển khoản, gồm tài khoản nhận, số tiền chuyển (tối thiểu 10,000 VND), lời nhắn và mã PIN giao dịch gồm 6 chữ số.
*   **[TransactionResponseDto.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/dto/TransactionResponseDto.java):** Định dạng lịch sử giao dịch gọn nhẹ trả về cho API sao kê, tự động phân tích và gắn nhãn giao dịch là `CREDIT` (nhận tiền) hay `DEBIT` (gửi tiền) dựa trên tài khoản của người dùng yêu cầu.
*   **[ChangePinRequest.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/dto/ChangePinRequest.java):** Dữ liệu đổi mã PIN giao dịch bao gồm mã PIN cũ và mã PIN mới.
*   **[UserResponseDto.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/dto/UserResponseDto.java) / [AccountResponse.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/dto/AccountResponse.java):** Chứa thông tin rút gọn về người dùng và số dư tài khoản ngân hàng để phản hồi nhanh.

---

### 6. Gói xử lý nghiệp vụ chính: `com.project_cuoimon.service`
Nơi tập trung 100% logic nghiệp vụ của ứng dụng (Trái tim của hệ thống).

*   **[CloudinaryService.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/CloudinaryService.java):**
    *   *Tác dụng:* Dịch vụ kết nối và tải ảnh lên Cloudinary.
    *   *Logic:* Đọc file ảnh thô gửi lên dưới dạng mảng byte và tải lên thư mục `/rikkei_bank` của Cloudinary, sau đó trích xuất lấy đường dẫn URL của bức ảnh trả về.
*   **[UserService.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/UserService.java) & [UserServiceImpl.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/UserServiceImpl.java):**
    *   *Tác dụng:* Xử lý các nghiệp vụ liên quan đến người dùng.
    *   *Logic các phương thức chính:*
        *   `registerCustomer`: Kiểm tra sự tồn tại của tên đăng nhập/email. Tiến hành mã hóa mật khẩu bằng BCrypt, gắn quyền `ROLE_CUSTOMER` mặc định và lưu người dùng mới.
        *   `uploadKyc`: Chặn nếu người dùng đã hoàn thành eKYC từ trước. Kiểm tra CCCD (`idNumber`) xem có bị trùng với người dùng khác không. Thực hiện tải ảnh lên Cloudinary và lưu hồ sơ với trạng thái `PENDING`.
        *   `approveKyc`: Tìm hồ sơ eKYC. Nếu trạng thái duyệt là `CONFIRM`, hệ thống cập nhật trạng thái `isKyc = true` cho User, đồng thời tự động sinh số tài khoản ngân hàng ngẫu nhiên 11 chữ số bắt đầu bằng đầu số `999` (có kiểm tra trùng lặp số tài khoản). Tạo thực thể `Account` mới với số tài khoản vừa sinh, tặng sẵn **50,000 VND** làm số dư ban đầu, mã hóa mã PIN mặc định `123456` và lưu xuống database.
        *   `forgotPassword`: Tìm người dùng và kiểm tra email có khớp. Sinh mật khẩu ngẫu nhiên độ dài 8 ký tự bằng cách cắt chuỗi UUID để đảm bảo bảo mật và cập nhật vào CSDL.
*   **[AccountService.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/AccountService.java) & [AccountServiceImpl.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/AccountServiceImpl.java):**
    *   *Tác dụng:* Xử lý nghiệp vụ tài khoản ngân hàng và chuyển tiền.
    *   *Logic các phương thức chính:*
        *   `getBalance`: Kiểm tra tài khoản ngân hàng liên kết với User. Nếu tài khoản bị khóa, ném lỗi `RuntimeException`. Ngược lại đóng gói số dư và số tài khoản trả về.
        *   `changePin`: Kiểm tra mã PIN cũ của người dùng có khớp với mã PIN mã hóa trong CSDL hay không, nếu khớp thì tiến hành băm mã PIN mới và lưu lại.
        *   `getStatement`: Tìm tài khoản ngân hàng của User, truy vấn toàn bộ lịch sử giao dịch liên quan từ `TransactionRepository` và đóng gói thành `TransactionResponseDto` phân trang.
        *   `transfer` (Nghiệp vụ chuyển tiền cốt lõi):
            1. Sử dụng chú thích `@Transactional` đảm bảo tính toàn vẹn (tất cả các bước đều thành công hoặc đều rollback nếu xảy ra lỗi).
            2. Tìm kiếm tài khoản gửi và tài khoản nhận. Để tránh lỗi **Khóa chết chéo (Deadlock)**, hệ thống sẽ thực hiện khóa tài khoản có số tài khoản nhỏ hơn trước bằng cơ chế Khóa bi quan (`LockModeType.PESSIMISTIC_WRITE`).
            3. Kiểm tra mã PIN giao dịch, kiểm tra số tiền chuyển phải >= 10,000 VND, kiểm tra số dư tài khoản gửi có đủ thực hiện giao dịch hay không.
            4. Thực hiện trừ tiền tài khoản gửi, cộng tiền tài khoản nhận.
            5. Khởi tạo đối tượng `Transaction` ghi lại thông tin giao dịch và lưu vào cơ sở dữ liệu.

---

### 7. Gói điểm tiếp nhận API: `com.project_cuoimon.controller`
Đóng vai trò điều hướng yêu cầu HTTP đến tầng nghiệp vụ và trả kết quả cho client.

*   **[AuthController.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/AuthController.java):**
    *   *Logic:* Chứa các API `/api/auth/register`, `/api/auth/login`, `/api/auth/forgot-password`, `/api/auth/logout` và `/api/auth/refreshtoken`. Thực hiện chuyển tiếp dữ liệu đến `UserService` để thực hiện xác thực và trả về JSON định dạng `ApiResponse`.
*   **[UserController.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/UserController.java):**
    *   *Logic:* Tiếp nhận các API nghiệp vụ của người dùng như `/api/v1/customer/kyc/upload` (gửi ảnh eKYC), `/api/v1/staff/kyc/approve/{id}` (phê duyệt eKYC - quyền STAFF), và `/api/v1/users/{id}/status` (Khóa/mở khóa tài khoản).
*   **[AccountController.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/AccountController.java):**
    *   *Logic:* Tiếp nhận các API tài khoản của khách hàng (yêu cầu phân quyền `ROLE_CUSTOMER`), bao gồm `/api/v1/customer/accounts/balance` (xem số dư), `/api/v1/customer/accounts/transfer` (chuyển khoản), `/api/v1/customer/accounts/statement` (xem sao kê), và `/api/v1/customer/accounts/change-pin` (đổi mã PIN).

---

### 8. Gói giám sát AOP: `com.project_cuoimon.aspect`
Áp dụng lập trình hướng khía cạnh (AOP) giúp tách biệt các nghiệp vụ bổ trợ như ghi log kiểm toán và đo hiệu năng khỏi mã nguồn chính.

*   **[AuditLoggingAspect.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/aspect/AuditLoggingAspect.java):**
    *   *Logic:* Rình xem khi nào phương thức chuyển tiền `transfer()` ở tầng Service chạy xong. Nếu chuyển khoản thành công (`@AfterReturning`), nó tự động ghi log thông tin số tiền, người gửi, người nhận ra console. Nếu giao dịch ném ra lỗi (`@AfterThrowing`), nó ghi log lỗi tương ứng.
*   **[PerformanceLoggingAspect.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/aspect/PerformanceLoggingAspect.java):**
    *   *Logic:* Sử dụng khuyên `@Around` bao quanh tất cả các phương thức ở tầng Controller và Service. Nó lưu lại mốc thời gian lúc bắt đầu chạy phương thức và thời điểm kết thúc phương thức, tính toán hiệu số và ghi log hiệu năng thực thi ra console để giúp phát hiện các hàm chạy chậm.

---

### 9. Gói xử lý lỗi tập trung: `com.project_cuoimon.exception`
Đảm bảo tính thống nhất trong phản hồi lỗi hệ thống.

*   **[GlobalExceptionHandler.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/exception/GlobalExceptionHandler.java):**
    *   *Logic:*
        *   Sử dụng chú thích `@ControllerAdvice` để can thiệp vào toàn bộ các Controller khi xảy ra lỗi.
        *   Khi ném ra `RuntimeException`, nó sẽ kiểm tra nội dung thông báo lỗi. Nếu là lỗi liên quan đến số dư, trùng lặp CCCD/Username, nó sẽ phản hồi mã HTTP `409 Conflict`. Ngược lại phản hồi mã HTTP `400 Bad Request`. Nội dung trả về là đối tượng `ApiResponse` thống nhất có `success = false`.
        *   Khi dữ liệu đầu vào vi phạm các ràng buộc `@Valid`, nó chuyển đổi thành cấu trúc JSON thông báo lỗi đầu vào chi tiết kèm trạng thái HTTP `400 Bad Request`.

---

## III. TỔNG KẾT VỀ CÁC FILE KIỂM THỬ (TEST SUITE)
Để bảo vệ dự án tốt nhất và đảm bảo mã nguồn chạy ổn định, hệ thống đã cài đặt **19 ca kiểm thử** thành công:
1.  **[ProjectCuoiMonApplicationTests.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/test/java/com/project_cuoimon/ProjectCuoiMonApplicationTests.java):** Chứa các ca kiểm thử tích hợp (Integration Tests) chạy cùng với cơ sở dữ liệu thật giả lập để xác định luồng Đăng ký tài khoản thành công và bắt lỗi Đăng ký trùng tên đăng nhập.
2.  **[UserServiceImplTest.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/test/java/com/project_cuoimon/service/UserServiceImplTest.java):** Chứa các ca kiểm thử đơn vị cho tầng dịch vụ người dùng như đăng ký thành công, chặn trùng lặp, duyệt eKYC tự tạo tài khoản.
3.  **[AccountServiceImplTest.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/test/java/com/project_cuoimon/service/AccountServiceImplTest.java):** Chứa các ca kiểm thử logic tài khoản ngân hàng, kiểm tra mã PIN giao dịch và kiểm tra tính hợp lệ khi chuyển tiền.
4.  **Các tệp Controller Test (ví dụ: AuthControllerTest, UserControllerTest, AccountControllerTest):** Kiểm tra xem các API Endpoint có phản hồi đúng mã trạng thái HTTP như 200, 201 khi truyền các tham số giả lập hay không.
