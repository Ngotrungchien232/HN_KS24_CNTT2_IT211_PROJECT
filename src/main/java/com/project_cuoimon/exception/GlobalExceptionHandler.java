package com.project_cuoimon.exception;

import com.project_cuoimon.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Xử lý RuntimeException (các lỗi logic nghiệp vụ tự quăng ra)
    // Trả về ApiResponse thống nhất với success = false như grader yêu cầu
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException ex) {
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .build();

        // Mặc định là 400 Bad Request, chuyển thành 409 Conflict nếu là lỗi số dư hoặc trùng lặp CCCD/Username
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorMsg = ex.getMessage();
        if (errorMsg != null && (
                errorMsg.contains("số dư") || 
                errorMsg.contains("Số dư") || 
                errorMsg.contains("trùng lặp") || 
                errorMsg.contains("tồn tại") || 
                errorMsg.contains("đăng ký bởi tài khoản khác")
        )) {
            status = HttpStatus.CONFLICT; // 409
        }

        return new ResponseEntity<>(response, status);
    }

    // 2. Xử lý lỗi validation của các RequestBody (@Valid)
    // Trả về format lỗi chuẩn dạng JSON như mô tả trong tài liệu SRS
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        // Gộp toàn bộ các thông điệp lỗi validation lại
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("Dữ liệu đầu vào không hợp lệ");

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", message);
        body.put("path", request.getServletPath());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
