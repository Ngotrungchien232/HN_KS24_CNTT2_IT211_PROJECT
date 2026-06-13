# KỊCH BẢN THUYẾT TRÌNH BẢO VỆ ĐỒ ÁN - HỆ THỐNG RIKKEI BANK
*(Tài liệu chuẩn bị cho kỳ bảo vệ đồ án tốt nghiệp - Mô tả chi tiết luồng xử lý của từng chức năng)*

Chào em! Để giúp em tự tin nhất trước các câu hỏi ngẫu nhiên từ Hội đồng chấm thi, thầy đã soạn thảo kịch bản thuyết trình chi tiết này cho **tất cả 12 chức năng của hệ thống**. 

Mỗi chức năng được trình bày đúng chuẩn cấu trúc mà thầy cô yêu cầu: **Điểm xuất phát -> API & Dữ liệu -> Xác thực -> Xử lý Service -> Tương tác Repository -> Kết quả trả về -> Vị trí mã nguồn**.

---

## MỤC LỤC 12 CHỨC NĂNG HỆ THỐNG
1. [Đăng ký tài khoản khách hàng mới](#1-đăng-ký-tài-khoản-khách-hàng-mới)
2. [Đăng nhập tài khoản & Nhận thẻ JWT](#2-đăng-nhập-tài-khoản--nhận-thẻ-jwt)
3. [Gửi hồ sơ định danh eKYC (Tải ảnh CCCD)](#3-gửi-hồ-sơ-định-danh-ekyc-tải-ảnh-cccd)
4. [Phê duyệt hồ sơ eKYC & Tự động mở tài khoản ngân hàng](#4-phê-duyệt-hồ-sơ-ekyc--tự-động-mở-tài-khoản-ngân-hàng)
5. [Xem số dư tài khoản ngân hàng](#5-xem-số-dư-tài-khoản-ngân-hàng)
6. [Thực hiện chuyển khoản (Nội bộ / Liên ngân hàng)](#6-thực-hiện-chuyển-khoản-nội-bộ--liên-ngân-hàng)
7. [Xem sao kê lịch sử giao dịch (Phân trang)](#7-xem-sao-kê-lịch-sử-giao-dịch-phân-trang)
8. [Đổi mã PIN giao dịch](#8-đổi-mã-pin-giao-dịch)
9. [Quên mật khẩu & Cấp mật khẩu tạm thời ngẫu nhiên](#9-quên-mật-khẩu--cấp-mật-khẩu-tạm-thời-ngẫu-nhiên)
10. [Đăng xuất (Logout & Vô hiệu hóa Token)](#10-đăng-xuất-logout--vô-hiệu-hóa-token)
11. [Làm mới Token đăng nhập (Refresh Token)](#11-làm-mới-token-đăng-nhập-refresh-token)
12. [Xem danh sách người dùng hệ thống](#12-xem-danh-sách-người-dùng-hệ-thống)

---

### 1. Đăng ký tài khoản khách hàng mới
*   **1. Điểm xuất phát:** Một khách hàng mới chưa có tài khoản muốn đăng ký thông tin vào hệ thống để bắt đầu sử dụng dịch vụ Rikkei Bank.
*   **2. API Controller & Dữ liệu truyền vào:**
    *   **API:** `POST /api/auth/register` (Không cần Token bảo mật).
    *   **Dữ liệu truyền vào:** Đối tượng JSON bọc trong lớp `RegisterRequest` gồm: `username`, `password`, `email`, `phoneNumber`.
*   **3. Xác thực dữ liệu tại Controller:**
    *   Sử dụng các chú thích kiểm hợp dữ liệu trong DTO: `@NotBlank` cho các trường bắt buộc, `@Email` để đảm bảo định dạng thư điện tử, `@Size` để kiểm tra độ dài mật khẩu và số điện thoại.
*   **4. Xử lý nghiệp vụ tại Service:**
    *   Controller gọi phương thức `registerCustomer(request)` của `UserService`.
    *   Service thực hiện kiểm tra logic nghiệp vụ: Gọi Repository kiểm tra xem `username` và `email` đã tồn tại trong hệ thống chưa. Nếu đã có, ném ra ngoại lệ `RuntimeException` (được `GlobalExceptionHandler` bắt và trả về mã HTTP `409 Conflict`).
    *   Nếu chưa tồn tại, thực hiện băm mật khẩu thô bằng thuật toán **BCrypt** (`passwordEncoder.encode()`).
    *   Tìm kiếm vai trò mặc định `ROLE_CUSTOMER` để gán cho người dùng mới.
*   **5. Tương tác với Repository & Database:**
    *   Gọi `userRepository.existsByUsername()` và `userRepository.existsByEmail()` để kiểm tra trùng lặp.
    *   Gọi `roleRepository.findByName("ROLE_CUSTOMER")` để lấy thông tin vai trò.
    *   Gọi `userRepository.save()` để lưu đối tượng `User` mới xuống bảng `users` trong MySQL.
*   **6. Kết quả xử lý trả về:** HTTP Status `201 Created` kèm theo đối tượng `ApiResponse` định dạng JSON báo thành công.
*   **7. Vị trí mã nguồn:**
    *   Controller: [AuthController.java - dòng 41: registerUser()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/AuthController.java#L41)
    *   Service: [UserServiceImpl.java - dòng 47: registerCustomer()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/UserServiceImpl.java#L47)

---

### 2. Đăng nhập tài khoản & Nhận thẻ JWT
*   **1. Điểm xuất phát:** Khách hàng hoặc nhân viên đã có tài khoản muốn đăng nhập hệ thống để nhận Token xác thực, truy cập các tài nguyên bảo mật.
*   **2. API Controller & Dữ liệu truyền vào:**
    *   **API:** `POST /api/auth/login` (Không cần Token bảo mật).
    *   **Dữ liệu truyền vào:** Đối tượng JSON bọc trong lớp `LoginRequest` gồm: `username`, `password`.
*   **3. Xác thực dữ liệu tại Controller:**
    *   Sử dụng `@Valid` kiểm tra dữ liệu đầu vào thông qua chú thích `@NotBlank` gắn trên `username` và `password`.
*   **4. Xử lý nghiệp vụ tại Service:**
    *   Controller gọi `authenticationManager.authenticate()` để kích hoạt luồng kiểm tra thông tin đăng nhập của Spring Security.
    *   Spring Security tự động gọi `UserDetailsServiceImpl.loadUserByUsername()` để lấy thông tin tài khoản từ MySQL, so sánh mật khẩu băm.
    *   *Kiểm tra bảo mật bổ sung:* `UserDetailsImpl.isEnabled()` được gọi để kiểm tra trường `isActive` của User. Nếu tài khoản bị khóa (`isActive = false`), từ chối đăng nhập.
    *   Nếu thông tin chính xác, gọi `JwtUtils.generateJwtToken()` tạo ra một chuỗi **AccessToken** (hạn 5 phút) để xác thực từng request.
    *   Đồng thời sinh một chuỗi ngẫu nhiên **RefreshToken** (hạn 24 giờ) dùng để gia hạn phiên làm việc.
*   **5. Tương tác với Repository & Database:**
    *   Gọi `userRepository.findByUsername()` để lấy thông tin kiểm tra mật khẩu.
    *   Gọi `refreshTokenRepository.save()` để lưu trữ hoặc cập nhật mã làm mới token trong cơ sở dữ liệu.
*   **6. Kết quả xử lý trả về:** HTTP Status `200 OK` kèm JSON chứa `accessToken`, `refreshToken`, tên đăng nhập, email, và danh sách các quyền hạn (`roles`).
*   **7. Vị trí mã nguồn:**
    *   Controller: [AuthController.java - dòng 56: authenticateUser()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/AuthController.java#L56)
    *   Bảo mật kiểm tra trạng thái: [UserDetailsImpl.java - dòng 58: isEnabled()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/security/UserDetailsImpl.java#L58)

---

### 3. Gửi hồ sơ định danh eKYC (Tải ảnh CCCD)
*   **1. Điểm xuất phát:** Khách hàng (`CUSTOMER`) muốn đăng tải hình ảnh thẻ CCCD/Passport và thông tin cá nhân lên hệ thống để yêu cầu nhân viên phê duyệt kích hoạt tài khoản ngân hàng.
*   **2. API Controller & Dữ liệu truyền vào:**
    *   **API:** `POST /api/v1/customer/kyc/upload` (Yêu cầu gửi kèm Token bảo mật với quyền `CUSTOMER`).
    *   **Dữ liệu truyền vào:** Gửi dưới dạng dữ liệu Form (`multipart/form-data`) gồm: ảnh CCCD (`file`), và các trường thông tin: `idNumber`, `fullName`, `dob` (ngày sinh), `sex`, `address`.
*   **3. Xác thực dữ liệu tại Controller:**
    *   Xác thực người dùng hiện tại qua `@AuthenticationPrincipal UserDetailsImpl`. Kiểm tra file ảnh không được trống.
*   **4. Xử lý nghiệp vụ tại Service:**
    *   Controller gọi `userService.uploadKyc()`.
    *   Service thực hiện kiểm tra logic:
        *   Nếu khách hàng đã hoàn thành KYC trước đó (`isKyc = true`), ném lỗi.
        *   Nếu khách hàng đang có một hồ sơ KYC ở trạng thái `PENDING` (chờ duyệt), chặn lại không cho tải đè.
        *   Kiểm tra số CCCD (`idNumber`) xem có bị trùng lặp với người dùng khác trong hệ thống không.
        *   Gọi dịch vụ `CloudinaryService.uploadFile()` để đẩy ảnh lên máy chủ đám mây Cloudinary và lấy lại URL đường dẫn ảnh an toàn.
*   **5. Tương tác với Repository & Database:**
    *   Gọi `kycProfileRepository.findByUser()` để tìm hồ sơ cũ của khách hàng.
    *   Gọi `kycProfileRepository.existsByIdNumber()` để kiểm tra trùng số định danh.
    *   Gọi `kycProfileRepository.save()` lưu thông tin hồ sơ KYC mới với trạng thái ban đầu là `PENDING` (chờ duyệt) và link ảnh Cloudinary xuống database.
*   **6. Kết quả xử lý trả về:** HTTP Status `200 OK` kèm đối tượng `ApiResponse` chứa thông tin chi tiết hồ sơ eKYC vừa nộp.
*   **7. Vị trí mã nguồn:**
    *   Controller: [UserController.java - dòng 33: uploadKyc()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/UserController.java#L33)
    *   Service: [UserServiceImpl.java - dòng 74: uploadKyc()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/UserServiceImpl.java#L74)

---

### 4. Phê duyệt hồ sơ eKYC & Tự động mở tài khoản ngân hàng
*   **1. Điểm xuất phát:** Nhân viên ngân hàng (`STAFF` hoặc `ADMIN`) đăng nhập và thực hiện kiểm duyệt hồ sơ định danh cá nhân của khách hàng gửi lên để quyết định phê duyệt hay từ chối.
*   **2. API Controller & Dữ liệu truyền vào:**
    *   **API:** `PUT /api/v1/staff/kyc/approve/{id}` (Yêu cầu Token quyền `STAFF` hoặc `ADMIN`).
    *   **Dữ liệu truyền vào:** `id` của hồ sơ KYC trên URL (`@PathVariable`), và tham số trạng thái duyệt `status` (`CONFIRM` hoặc `REJECT`) dưới dạng Query Parameter.
*   **3. Xác thực dữ liệu tại Controller:**
    *   Kiểm tra xem mã hồ sơ truyền vào có hợp lệ không.
*   **4. Xử lý nghiệp vụ tại Service:**
    *   Controller gọi `userService.approveKyc(id, status)`.
    *   Service thực hiện kiểm tra logic:
        *   Tìm hồ sơ eKYC theo ID. Nếu không có, ném lỗi.
        *   Kiểm tra nếu hồ sơ đã được xử lý trước đó (trạng thái khác `PENDING`), chặn lại.
        *   Cập nhật trạng thái duyệt (`CONFIRM` hoặc `REJECT`).
        *   **Nếu duyệt thành công (`CONFIRM`):**
            *   Đánh dấu người dùng đã hoàn tất xác thực: `user.setIsKyc(true)`.
            *   Tự động khởi tạo tài khoản ngân hàng thanh toán cho khách hàng: Sinh ngẫu nhiên số tài khoản 11 số bắt đầu bằng `999` (có vòng lặp kiểm tra trùng lặp trong DB).
            *   Cộng sẵn **50,000 VND** làm số dư tối thiểu ban đầu.
            *   Băm và thiết lập mã PIN giao dịch mặc định là `123456`.
*   **5. Tương tác với Repository & Database:**
    *   Gọi `kycProfileRepository.findById()` để lấy thông tin hồ sơ.
    *   Gọi `accountRepository.existsByAccountNumber()` để kiểm tra số tài khoản ngân hàng trùng lặp.
    *   Gọi `userRepository.save()` và `accountRepository.save()` để ghi nhận thông tin kích hoạt tài khoản xuống DB.
*   **6. Kết quả xử lý trả về:** HTTP Status `200 OK` kèm thông tin hồ sơ KYC đã được duyệt.
*   **7. Vị trí mã nguồn:**
    *   Controller: [UserController.java - dòng 48: approveKyc()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/UserController.java#L48)
    *   Service: [UserServiceImpl.java - dòng 114: approveKyc()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/UserServiceImpl.java#L114)

---

### 5. Xem số dư tài khoản ngân hàng
*   **1. Điểm xuất phát:** Khách hàng (`CUSTOMER`) sau khi đăng nhập muốn kiểm tra số dư hiện tại và số tài khoản thanh toán của mình.
*   **2. API Controller & Dữ liệu truyền vào:**
    *   **API:** `GET /api/v1/customer/accounts/balance` (Yêu cầu Token quyền `CUSTOMER`).
    *   **Dữ liệu truyền vào:** Không truyền gì ngoài Token xác thực trên Header.
*   **3. Xác thực dữ liệu tại Controller:**
    *   Hệ thống xác thực token và trích xuất thông tin người dùng đang đăng nhập qua đối tượng `@AuthenticationPrincipal UserDetailsImpl`.
*   **4. Xử lý nghiệp vụ tại Service:**
    *   Controller gọi `accountService.getBalance(user)`.
    *   Service thực hiện kiểm tra logic:
        *   Truy vấn thông tin tài khoản ngân hàng gắn liền với User này.
        *   Kiểm tra xem tài khoản ngân hàng của khách hàng có bị khóa hoạt động hay không. Nếu bị khóa (`active = false`), ném lỗi chặn truy cập.
*   **5. Tương tác với Repository & Database:**
    *   Gọi `accountRepository.findByUser()` để truy vấn tài khoản thanh toán của người dùng hiện tại từ bảng `accounts` trong MySQL.
*   **6. Kết quả xử lý trả về:** HTTP Status `200 OK` kèm đối tượng `ApiResponse` chứa số tài khoản, số dư hiện có và loại tiền tệ (VND).
*   **7. Vị trí mã nguồn:**
    *   Controller: [AccountController.java - dòng 31: getBalance()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/AccountController.java#L31)
    *   Service: [AccountServiceImpl.java - dòng 34: getBalance()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/AccountServiceImpl.java#L34)

---

### 6. Thực hiện chuyển khoản (Nội bộ / Liên ngân hàng)
*   **1. Điểm xuất phát:** Khách hàng (`CUSTOMER`) muốn chuyển một số tiền từ tài khoản của mình sang một tài khoản khác trong hệ thống.
*   **2. API Controller & Dữ liệu truyền vào:**
    *   **API:** `POST /api/v1/customer/accounts/transfer` (Yêu cầu Token quyền `CUSTOMER`).
    *   **Dữ liệu truyền vào:** Đối tượng JSON bọc trong lớp `TransferRequest` gồm: `targetAccountNumber` (tài khoản nhận), `amount` (số tiền chuyển), `description` (nội dung), `transactionPin` (mã PIN 6 số).
*   **3. Xác thực dữ liệu tại Controller:**
    *   Sử dụng `@Valid` kiểm tra dữ liệu đầu vào: `targetAccountNumber` và `transactionPin` không được trống, số tiền chuyển phải tối thiểu là `10,000` VND (`@DecimalMin`).
*   **4. Xử lý nghiệp vụ tại Service:**
    *   Controller gọi `accountService.transfer(user, request)`.
    *   Service thực hiện xử lý trong phạm vi `@Transactional` (đảm bảo tính toàn vẹn dữ liệu, nếu một bước lỗi toàn bộ giao dịch sẽ Rollback):
        1. Tìm kiếm tài khoản gửi (của User đang đăng nhập) và tài khoản nhận (theo số tài khoản truyền lên).
        2. **Chống Deadlock:** So sánh chuỗi số tài khoản gửi và nhận, tiến hành truy vấn khóa tài khoản có số tài khoản nhỏ hơn trước bằng cơ chế **Khóa bi quan (Pessimistic Lock)**.
        3. Kiểm tra mã PIN giao dịch truyền lên có khớp với mã PIN đã băm trong database không.
        4. Kiểm tra số dư tài khoản gửi có đủ để trừ tiền giao dịch không.
        5. Thực hiện trừ tiền tài khoản gửi, cộng tiền tương ứng vào tài khoản nhận.
        6. Tạo đối tượng `Transaction` ghi nhận lịch sử giao dịch.
*   **5. Tương tác với Repository & Database:**
    *   Gọi `accountRepository.findByAccountNumber()` có cài đặt `@Lock(LockModeType.PESSIMISTIC_WRITE)` để khóa tài khoản gửi và nhận trong MySQL.
    *   Gọi `accountRepository.save()` để cập nhật số dư mới cho cả 2 tài khoản.
    *   Gọi `transactionRepository.save()` lưu giao dịch mới vào bảng `transactions`.
*   **6. Kết quả xử lý trả về:** HTTP Status `200 OK` kèm chi tiết hóa đơn giao dịch vừa chuyển khoản thành công.
*   **7. Vị trí mã nguồn:**
    *   Controller: [AccountController.java - dòng 50: transfer()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/AccountController.java#L50)
    *   Service: [AccountServiceImpl.java - dòng 77: transfer()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/AccountServiceImpl.java#L77)
    *   Khóa bi quan: [AccountRepository.java - dòng 20: findByAccountNumber()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/repository/AccountRepository.java#L20)

---

### 7. Xem sao kê lịch sử giao dịch (Phân trang)
*   **1. Điểm xuất phát:** Khách hàng (`CUSTOMER`) muốn xem lại lịch sử các giao dịch gửi tiền hoặc nhận tiền của mình theo định dạng phân trang, sắp xếp theo thời gian mới nhất.
*   **2. API Controller & Dữ liệu truyền vào:**
    *   **API:** `GET /api/v1/customer/accounts/statement` (Yêu cầu Token quyền `CUSTOMER`).
    *   **Dữ liệu truyền vào:** Tham số phân trang trên URL (`page` mặc định là `0`, `size` mặc định là `10`).
*   **3. Xác thực dữ liệu tại Controller:**
    *   Xác thực Token người dùng và nhận đối tượng `UserDetailsImpl` đang đăng nhập.
*   **4. Xử lý nghiệp vụ tại Service:**
    *   Controller gọi `accountService.getStatement(user, pageable)`.
    *   Service truy vấn tài khoản ngân hàng của User.
    *   Gọi Repository tìm kiếm danh sách giao dịch có tài khoản gửi HOẶC tài khoản nhận trùng khớp với tài khoản khách hàng.
    *   Ánh xạ (map) danh sách giao dịch sang lớp DTO `TransactionResponseDto`. Logic tại DTO này sẽ so sánh: Nếu tài khoản khách hàng trùng với tài khoản gửi, gắn nhãn loại giao dịch là `DEBIT` (trừ tiền), ngược lại gắn nhãn là `CREDIT` (nhận tiền).
*   **5. Tương tác với Repository & Database:**
    *   Gọi `transactionRepository.findBySenderAccountOrReceiverAccountOrderByTimestampDesc()` để lấy danh sách giao dịch phân trang từ MySQL.
*   **6. Kết quả xử lý trả về:** HTTP Status `200 OK` kèm đối tượng `ApiResponse` chứa trang dữ liệu lịch sử giao dịch (`Page` object).
*   **7. Vị trí mã nguồn:**
    *   Controller: [AccountController.java - dòng 70: getStatement()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/AccountController.java#L70)
    *   Service: [AccountServiceImpl.java - dòng 45: getStatement()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/AccountServiceImpl.java#L45)
    *   Logic phân loại CREDIT/DEBIT: [TransactionResponseDto.java - dòng 20: constructor mapping](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/dto/TransactionResponseDto.java#L20)

---

### 8. Đổi mã PIN giao dịch
*   **1. Điểm xuất phát:** Khách hàng (`CUSTOMER`) muốn thay đổi mã PIN giao dịch (mã PIN mặc định `123456` sau khi duyệt eKYC) sang mã PIN bảo mật cá nhân mới.
*   **2. API Controller & Dữ liệu truyền vào:**
    *   **API:** `PUT /api/v1/customer/accounts/change-pin` (Yêu cầu Token quyền `CUSTOMER`).
    *   **Dữ liệu truyền vào:** Đối tượng JSON bọc trong lớp `ChangePinRequest` gồm: `oldPin` (mã PIN hiện tại) và `newPin` (mã PIN mới).
*   **3. Xác thực dữ liệu tại Controller:**
    *   Sử dụng `@Valid` kiểm tra dữ liệu đầu vào: Cả 2 mã PIN bắt buộc không được trống, phải gồm chính xác 6 chữ số (`@Size(min=6, max=6)`).
*   **4. Xử lý nghiệp vụ tại Service:**
    *   Controller gọi `accountService.changePin(user, request)`.
    *   Service tìm tài khoản của người dùng.
    *   Kiểm tra xem mã PIN cũ (`oldPin`) nhập vào có khớp với mã PIN đã được mã hóa trong cơ sở dữ liệu hay không thông qua phương thức `passwordEncoder.matches()`. Nếu sai, ném lỗi.
    *   Nếu đúng, tiến hành mã hóa mã PIN mới bằng thuật toán BCrypt và lưu đè lên dữ liệu cũ.
*   **5. Tương tác với Repository & Database:**
    *   Gọi `accountRepository.findByUser()` để lấy tài khoản hiện tại.
    *   Gọi `accountRepository.save()` để ghi nhận mã PIN mới đã mã hóa xuống DB.
*   **6. Kết quả xử lý trả về:** HTTP Status `200 OK` kèm thông báo đổi mã PIN thành công.
*   **7. Vị trí mã nguồn:**
    *   Controller: [AccountController.java - dòng 92: changePin()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/AccountController.java#L92)
    *   Service: [AccountServiceImpl.java - dòng 58: changePin()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/AccountServiceImpl.java#L58)

---

### 9. Quên mật khẩu & Cấp mật khẩu tạm thời ngẫu nhiên
*   **1. Điểm xuất phát:** Người dùng bị quên mật khẩu đăng nhập tài khoản và muốn hệ thống cấp lại một mật khẩu tạm thời mới để đăng nhập.
*   **2. API Controller & Dữ liệu truyền vào:**
    *   **API:** `POST /api/auth/forgot-password` (Không cần Token bảo mật).
    *   **Dữ liệu truyền vào:** Đối tượng JSON bọc trong lớp `ForgotPasswordRequest` gồm: `username`, `email` (email đã đăng ký của tài khoản).
*   **3. Xác thực dữ liệu tại Controller:**
    *   Sử dụng các kiểm hợp `@NotBlank` và `@Email` trên Request DTO để đảm bảo dữ liệu truyền lên đầy đủ và đúng định dạng thư điện tử.
*   **4. Xử lý nghiệp vụ tại Service:**
    *   Controller gọi `userService.forgotPassword(request)`.
    *   Service thực hiện kiểm tra logic:
        *   Truy vấn User dựa trên `username`. Nếu không tìm thấy, ném ngoại lệ thông báo tài khoản không tồn tại.
        *   Kiểm tra xem địa chỉ email gửi lên có khớp chính xác với email đã đăng ký của tài khoản này trong database không. Nếu không khớp, ném ngoại lệ.
        *   Sinh ngẫu nhiên một chuỗi mật khẩu tạm thời có độ dài 8 ký tự bằng cách cắt mã băm UUID: `UUID.randomUUID().toString().substring(0, 8)`.
        *   Mã hóa mật khẩu tạm thời này bằng BCrypt và cập nhật cho User.
*   **5. Tương tác với Repository & Database:**
    *   Gọi `userRepository.findByUsername()` để tìm kiếm người dùng.
    *   Gọi `userRepository.save()` để cập nhật mật khẩu đã mã hóa mới xuống bảng `users` trong MySQL.
*   **6. Kết quả xử lý trả về:** HTTP Status `200 OK` kèm theo chuỗi mật khẩu tạm thời vừa sinh trong thuộc tính dữ liệu trả về để người dùng sử dụng.
*   **7. Vị trí mã nguồn:**
    *   Controller: [AuthController.java - dòng 110: forgotPassword()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/AuthController.java#L110)
    *   Service: [UserServiceImpl.java - dòng 202: forgotPassword()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/UserServiceImpl.java#L202)

---

### 10. Đăng xuất (Logout & Vô hiệu hóa Token)
*   **1. Điểm xuất phát:** Người dùng muốn hủy phiên làm việc hiện tại, vô hiệu hóa AccessToken để không ai có thể sử dụng lại token cũ để truy cập trái phép.
*   **2. API Controller & Dữ liệu truyền vào:**
    *   **API:** `POST /api/auth/logout` (Yêu cầu gửi kèm Token bảo mật hiện tại).
    *   **Dữ liệu truyền vào:** Trích xuất Access Token trực tiếp từ Header `Authorization`.
*   **3. Xác thực dữ liệu tại Controller:**
    *   Hệ thống tự động xác thực token gửi lên có hợp lệ hay không trước khi cho vào API xử lý.
*   **4. Xử lý nghiệp vụ tại Service:**
    *   Controller trích xuất chuỗi token từ yêu cầu.
    *   Gọi phương thức lưu chuỗi token này vào thực thể danh sách đen `TokenBlackList`.
    *   Khi token bị đưa vào bảng danh sách đen, ở các request tiếp theo, bộ lọc `AuthTokenFilter` quét qua thấy token nằm trong blacklist sẽ từ chối xử lý và chặn người dùng lại ngay lập tức.
*   **5. Tương tác với Repository & Database:**
    *   Gọi `tokenBlackListRepository.save()` để lưu token đã đăng xuất xuống bảng `token_blacklist` trong MySQL.
*   **6. Kết quả xử lý trả về:** HTTP Status `200 OK` kèm thông báo người dùng đăng xuất thành công.
*   **7. Vị trí mã nguồn:**
    *   Controller: [AuthController.java - dòng 98: logoutUser()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/AuthController.java#L98)
    *   Kiểm tra chặn từ bộ lọc: [AuthTokenFilter.java - dòng 43: doFilterInternal()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/security/AuthTokenFilter.java#L43)

---

### 11. Làm mới Token đăng nhập (Refresh Token)
*   **1. Điểm xuất phát:** Access Token cũ của người dùng đã hết hạn (sau 5 phút), ứng dụng client âm thầm mang Refresh Token gửi lên hệ thống để gia hạn phiên làm việc mà không cần người dùng nhập lại mật khẩu.
*   **2. API Controller & Dữ liệu truyền vào:**
    *   **API:** `POST /api/auth/refreshtoken` (Không cần gửi Access Token cũ trên Header).
    *   **Dữ liệu truyền vào:** Đối tượng JSON chứa mã `refreshToken` bọc trong lớp DTO `TokenRefreshRequest`.
*   **3. Xác thực dữ liệu tại Controller:**
    *   Sử dụng `@Valid` kiểm tra dữ liệu đầu vào không được để trống.
*   **4. Xử lý nghiệp vụ tại Service:**
    *   Controller tìm kiếm Refresh Token trong CSDL.
    *   Kiểm tra xem Refresh Token có hết hạn sử dụng hay chưa (thời hạn 24 giờ kể từ lúc tạo). Nếu hết hạn, tiến hành xóa token đó khỏi DB và ném lỗi yêu cầu đăng nhập lại.
    *   Nếu hợp lệ, giải mã Refresh Token để lấy thực thể `User`, gọi `JwtUtils.generateJwtToken()` tạo một Access Token mới tinh và trả về cho client.
*   **5. Tương tác với Repository & Database:**
    *   Gọi `refreshTokenRepository.findByToken()` để tìm kiếm token.
    *   Gọi `refreshTokenRepository.delete()` nếu token đã quá hạn.
*   **6. Kết quả xử lý trả về:** HTTP Status `200 OK` kèm mã Access Token mới và Refresh Token hiện tại.
*   **7. Vị trí mã nguồn:**
    *   Controller: [AuthController.java - dòng 77: refreshToken()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/AuthController.java#L77)

---

### 12. Xem danh sách người dùng hệ thống
*   **1. Điểm xuất phát:** Quản trị viên (`ADMIN`) hoặc Nhân viên (`STAFF`) muốn truy vấn danh sách toàn bộ khách hàng và trạng thái trong hệ thống để quản lý.
*   **2. API Controller & Dữ liệu truyền vào:**
    *   **API:** `GET /api/v1/users` (Yêu cầu Token có quyền `ADMIN` hoặc `STAFF`).
    *   **Dữ liệu truyền vào:** Các tham số phân trang trên URL: `page` (mặc định `0`), `size` (mặc định `10`).
*   **3. Xác thực dữ liệu tại Controller:**
    *   Xác định quyền truy cập của Token qua cấu hình Spring Security.
*   **4. Xử lý nghiệp vụ tại Service:**
    *   Controller gọi `userService.getAllUsers(pageable)`.
    *   Service chuyển tiếp yêu cầu xuống Repository.
    *   **Điểm tối ưu hóa hiệu năng:** Phương thức truy vấn sử dụng JPQL Constructor Projection để ánh xạ trực tiếp các cột cần thiết từ DB sang DTO `UserResponseDto` ngay tại câu truy vấn MySQL, giúp tối ưu dung lượng RAM máy chủ (không cần tải toàn bộ thực thể User nặng nề lên RAM rồi mới map).
*   **5. Tương tác với Repository & Database:**
    *   Gọi `userRepository.findAllUsersProjected(pageable)` thực thi câu lệnh JPQL để lấy về danh sách khách hàng phân trang từ MySQL.
*   **6. Kết quả xử lý trả về:** HTTP Status `200 OK` kèm theo danh sách người dùng phân trang dạng JSON.
*   **7. Vị trí mã nguồn:**
    *   Controller: [UserController.java - dòng 63: getAllUsers()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/controller/UserController.java#L63)
    *   Service: [UserServiceImpl.java - dòng 159: getAllUsers()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/service/UserServiceImpl.java#L159)
    *   Câu truy vấn tối ưu: [UserRepository.java - dòng 18: findAllUsersProjected()](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/repository/UserRepository.java#L18)

---

## IV. CÁC CÂU HỎI PHỤ "HÁT HÓC" HỘI ĐỒNG RẤT HAY HỎI (CÁCH TRẢ LỜI CỦA EM)

> [!IMPORTANT]
> **1. Câu hỏi:** *"Em dùng cơ chế gì để ghi nhận vết giao dịch và đo hiệu năng hệ thống mà không làm bẩn code nghiệp vụ chính?"*
> *   **Trả lời:** Em sử dụng **Lập trình hướng khía cạnh (Spring AOP)**. Em tách biệt logic giám sát ra các lớp Aspect độc lập:
>     *   [AuditLoggingAspect.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/aspect/AuditLoggingAspect.java) dùng khuyên `@AfterReturning` và `@AfterThrowing` để ghi lại log kiểm toán giao dịch chuyển tiền thành công hay thất bại.
>     *   [PerformanceLoggingAspect.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/aspect/PerformanceLoggingAspect.java) dùng khuyên `@Around` để đo thời gian chạy của các hàm ở tầng Controller và Service, ghi log hiệu năng ra console.

> [!WARNING]
> **2. Câu hỏi:** *"Tại sao trong hàm chuyển tiền em lại dùng Khóa bi quan (Pessimistic Lock) và xử lý sắp xếp số tài khoản nhỏ hơn trước?"*
> *   **Trả lời:** 
>     *   Em dùng **Khóa bi quan** `@Lock(LockModeType.PESSIMISTIC_WRITE)` để khóa các dòng dữ liệu tài khoản gửi/nhận trong MySQL khi giao dịch đang diễn ra. Việc này nhằm ngăn chặn lỗi *Double-spending* (tiêu kép) khi hai yêu cầu rút tiền diễn ra đồng thời.
>     *   Em thực hiện so sánh và khóa tài khoản có số tài khoản nhỏ hơn trước là để **tránh lỗi Deadlock (Khóa chết chéo nhau)**. Nếu không sắp xếp, giao dịch 1 khóa tài khoản A rồi đợi tài khoản B; cùng lúc giao dịch 2 khóa tài khoản B rồi đợi tài khoản A -> Cả hai giao dịch sẽ chờ nhau mãi mãi làm hệ thống bị treo. Sắp xếp thứ tự khóa giúp triệt tiêu hoàn toàn lỗi Deadlock này.

> [!TIP]
> **3. Câu hỏi:** *"Nếu người dùng gửi dữ liệu sai định dạng hoặc không hợp lệ, hệ thống của em xử lý tập trung như thế nào?"*
> *   **Trả lời:** Dự án của em đã cài đặt lớp xử lý ngoại lệ toàn cục [GlobalExceptionHandler.java](file:///D:/HN_KS24_CNTT2_IT211_JAVA_WEB_SERVICE/PROJECT_CuoiMon/Project_CuoiMon/src/main/java/com/project_cuoimon/exception/GlobalExceptionHandler.java) bằng chú thích `@ControllerAdvice`. Khi có bất kỳ lỗi logic hoặc lỗi validate dữ liệu xảy ra, bộ xử lý này sẽ tự động bắt lấy ngoại lệ, đóng gói thông điệp lỗi dạng JSON chuẩn `ApiResponse` và gửi lại cho client kèm theo các HTTP status phù hợp như `400 Bad Request` hoặc `409 Conflict`.
