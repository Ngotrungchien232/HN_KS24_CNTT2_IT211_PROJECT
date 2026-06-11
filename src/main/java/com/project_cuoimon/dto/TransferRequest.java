package com.project_cuoimon.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransferRequest {

    @NotBlank(message = "Số tài khoản nhận không được để trống")
    private String targetAccountNumber;

    @NotNull(message = "Số tiền chuyển không được để trống")
    @DecimalMin(value = "10000.00", message = "Số tiền chuyển tối thiểu là 10,000 VND")
    private BigDecimal amount;

    @Size(max = 255, message = "Lời nhắn tối đa 255 ký tự")
    private String description;

    @NotBlank(message = "Mã PIN giao dịch không được để trống")
    @Size(min = 6, max = 6, message = "Mã PIN giao dịch phải gồm 6 chữ số")
    private String transactionPin;
}
