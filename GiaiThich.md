# CẨM NANG GIẢI THÍCH TOÀN BỘ CẤU TRÚC DỰ ÁN RIKKEI BANK (DAY 1)

Chào em! Đây là tài liệu chi tiết giải thích ý nghĩa, nhiệm vụ của từng thư mục (package) và từng file mã nguồn hiện có trong dự án của em. 

Thầy viết tài liệu này bằng ngôn ngữ đơn giản, kèm các ví dụ ẩn dụ thực tế để em dễ dàng ôn tập và hiểu bản chất hệ thống.

---

## 1. Tổng quan cấu trúc thư mục (Packages)

Dự án của chúng ta tuân theo cấu trúc **3 Layer Architecture (Kiến trúc 3 lớp)** chuẩn doanh nghiệp:

*   **`controller` (Tầng trình diễn / Presentation Layer):** Tiếp nhận yêu cầu (Requests) từ khách hàng qua giao diện mạng và trả về kết quả (JSON).
*   **`service` (Tầng nghiệp vụ / Business Logic Layer):** Nơi chứa chất xám của dự án, tính toán logic và xử lý nghiệp vụ chính.
*   **`repository` (Tầng truy cập dữ liệu / Data Access Layer):** Nơi giao tiếp trực tiếp với cơ sở dữ liệu MySQL (đọc/ghi dữ liệu).
*   **`entity` (Tầng dữ liệu / Data Models):** Các lớp Java đại diện cho các bảng trong database MySQL.
*   **`dto` (Data Transfer Objects):** Các phong bì chứa dữ liệu gửi đi hoặc nhận về, giúp bảo mật dữ liệu.
*   **`config` & `security`:** Các bộ phận thiết lập hệ thống và bảo mật (JWT, phân quyền).

---

## 2. Chi tiết từng File trong dự án

### 2.1. Thư mục gốc dự án (Project Root)
*   **`build.gradle`:** File cấu hình quản lý thư viện của Gradle. Nó chứa thông tin phiên bản Java, các thư viện ta sử dụng (Spring Boot, Security, JPA, JWT, Cloudinary...).
*   **`application.properties`:** File chứa các thông số cài đặt hệ thống như cổng chạy Server (`8080`), tài khoản mật khẩu kết nối database MySQL, thông số giới hạn file upload, và mã khóa bảo mật JWT.
*   **`ProjectCuoiMonApplication.java`:** File chạy chính (Entry Point) của dự án. Chứa hàm `main` để khởi động toàn bộ ứng dụng Spring Boot.

---

### 2.2. Package `com.project_cuoimon.config` (Cấu hình hệ thống)
*   **`SecurityConfig.java`:** 
    *   *Nhiệm vụ:* Nơi thiết lập luật lệ bảo mật. Nó chỉ ra API nào được mở tự do (Đăng ký, Đăng nhập), API nào bắt buộc phải có vai trò tương ứng mới được vào (CUSTOMER, STAFF, ADMIN).
    *   *Ẩn dụ:* Đóng vai trò như **Bản vẽ sơ đồ bảo vệ tòa nhà**.
*   **`CloudinaryConfig.java`:**
    *   *Nhiệm vụ:* Thiết lập cấu hình kết nối tài khoản đám mây Cloudinary của em để hệ thống có quyền upload ảnh lên đó.
*   **`DatabaseSeeder.java`:**
    *   *Nhiệm vụ:* Khi ứng dụng vừa bật lên, lớp này sẽ kiểm tra xem database đã có 3 vai trò (`ROLE_ADMIN`, `ROLE_STAFF`, `ROLE_CUSTOMER`) chưa. Nếu chưa có, nó tự động chèn vào database để hệ thống không bị lỗi thiếu quyền.

---

### 2.3. Package `com.project_cuoimon.security` (Lực lượng bảo vệ JWT)
*   **`AuthEntryPointJwt.java`:**
    *   *Nhiệm vụ:* Bắt các yêu cầu truy cập trái phép (thiếu token, sai token) và định dạng lỗi thành chuỗi JSON đẹp mắt để trả về cho người dùng (lỗi 401 Unauthorized).
    *   *Ẩn dụ:* **Người bảo vệ từ chối lịch sự** ở cổng phụ.
*   **`AuthTokenFilter.java`:**
    *   *Nhiệm vụ:* Chặn mọi request gửi lên, kiểm tra xem người dùng có gửi kèm Token hợp lệ không, token có bị đưa vào danh sách đen (Blacklist) do đã Logout không. Nếu hợp lệ, nó sẽ cấp quyền tạm thời cho request đi tiếp.
    *   *Ẩn dụ:* **Trạm kiểm soát an ninh sân bay**.
*   **`JwtUtils.java`:**
    *   *Nhiệm vụ:* Chứa các hàm tiện ích để: tạo mới AccessToken/RefreshToken, giải mã token để lấy username và kiểm tra xem token còn hạn hay không.
    *   *Ẩn dụ:* **Máy quét mã vạch kiểm tra vé**.
*   **`UserDetailsImpl.java`:**
    *   *Nhiệm vụ:* Bọc đối tượng `User` của chúng ta thành đối tượng `UserDetails` mà Spring Security có thể hiểu và làm việc được.
*   **`UserDetailsServiceImpl.java`:**
    *   *Nhiệm vụ:* Cung cấp hàm tìm kiếm người dùng trong database bằng username để phục vụ cho bộ lọc bảo mật.

---

### 2.4. Package `com.project_cuoimon.entity` (Bản thiết kế database)
Các file này ánh xạ trực tiếp thành các bảng dưới MySQL:
*   **`Role.java`:** Bản thiết kế bảng vai trò (`roles`).
*   **`User.java`:** Bản thiết kế bảng người dùng (`users`).
*   **`KycProfile.java`:** Bản thiết kế bảng hồ sơ định danh (`kyc_profiles`).
*   **`Status.java`:** Enum định nghĩa 3 trạng thái eKYC duy nhất: `PENDING` (chờ duyệt), `CONFIRM` (đã duyệt), `REJECT` (từ chối).
*   **`RefreshToken.java`:** Bản thiết kế bảng chứa token xoay vòng để cấp lại AccessToken (`refresh_tokens`).
*   **`TokenBlackList.java`:** Bản thiết kế bảng chứa các AccessToken đã bị vô hiệu hóa khi người dùng bấm Logout (`token_blacklist`).
*   **`Account.java`:** Bản thiết kế bảng tài khoản thanh toán ngân hàng (`accounts`).
*   **`Transaction.java`:** Bản thiết kế bảng lịch sử giao dịch chuyển tiền (`transactions`).

---

### 2.5. Package `com.project_cuoimon.repository` (Kết nối Database)
Các interface này kế thừa `JpaRepository` để Hibernate tự động sinh ra các câu lệnh SQL truy vấn database MySQL:
*   **`RoleRepository` / `UserRepository` / `KycProfileRepository` / `RefreshTokenRepository` / `TokenBlackListRepository` / `AccountRepository`:**
    *   *Nhiệm vụ:* Cung cấp các hàm tìm kiếm, lưu trữ, kiểm tra tồn tại dữ liệu.
    *   *Đặc biệt (`UserRepository`):* Chứa câu lệnh truy vấn JPQL dùng **Constructor Projection** giúp chỉ lấy đúng các cột cần thiết để hiển thị danh sách người dùng, tối ưu hóa dung lượng RAM.

---

### 2.6. Package `com.project_cuoimon.dto` (Phong bì truyền dữ liệu)
Chứa các đối tượng gọn nhẹ dùng để đóng gói dữ liệu gửi đi và nhận về:
*   **`ApiResponse.java`:** Cấu trúc chuẩn trả về cho mọi API (`success`, `message`, `data`).
*   **`LoginRequest.java`:** Chứa thông tin đăng nhập gửi lên (username, password).
*   **`RegisterRequest.java`:** Chứa thông tin đăng ký gửi lên (username, password, email, phone).
*   **`JwtResponse.java`:** Chứa dữ liệu trả về khi đăng nhập thành công (AccessToken, RefreshToken, tên người dùng, quyền...).
*   **`TokenRefreshRequest.java` & `TokenRefreshResponse.java`:** Phục vụ nghiệp vụ xin cấp AccessToken mới bằng RefreshToken.
*   **`UserResponseDto.java`:** Chứa thông tin rút gọn của người dùng để trả về API danh sách (được nạp trực tiếp qua JPQL).
*   **`AccountResponse.java`:** Chứa thông tin số dư tài khoản trả về cho API vấn tin số dư.

---

### 2.7. Package `com.project_cuoimon.service` (Khối xử lý nghiệp vụ chính)
*   **`CloudinaryService.java`:** Xử lý việc upload ảnh file vật lý lên đám mây Cloudinary và trả về đường dẫn URL.
*   **`UserService.java` & `UserServiceImpl.java`:**
    *   *Nhiệm vụ:* Triển khai logic đăng ký tài khoản (mã hóa mật khẩu), logic gửi yêu cầu eKYC (gọi dịch vụ upload ảnh), logic phê duyệt eKYC (duyệt xong thì tự động mở tài khoản ngân hàng và tặng 50.000 VND), và lấy danh sách người dùng phân trang.
*   **`AccountService.java` & `AccountServiceImpl.java`:**
    *   *Nhiệm vụ:* Triển khai logic tìm kiếm tài khoản ngân hàng của khách hàng để lấy ra số dư hiện tại.

---

### 2.8. Package `com.project_cuoimon.controller` (Cổng API đầu cuối)
*   **`AuthController.java`:** Định nghĩa các API tiếp nhận yêu cầu Đăng ký, Đăng nhập, Đăng xuất, Làm mới token.
*   **`UserController.java`:** Định nghĩa các API tiếp nhận yêu cầu Khách hàng upload CCCD eKYC, Nhân viên phê duyệt eKYC, và Admin xem danh sách người dùng.
*   **`AccountController.java`:** Định nghĩa API tiếp nhận yêu cầu Vấn tin số dư tài khoản của khách hàng.
