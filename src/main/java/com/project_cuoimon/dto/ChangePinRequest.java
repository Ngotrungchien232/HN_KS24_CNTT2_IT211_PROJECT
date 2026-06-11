package com.project_cuoimon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePinRequest {

    @NotBlank(message = "Mã PIN cũ không được để trống")
    @Size(min = 6, max = 6, message = "Mã PIN phải gồm 6 chữ số")
    private String oldPin;

    @NotBlank(message = "Mã PIN mới không được để trống")
    @Size(min = 6, max = 6, message = "Mã PIN phải gồm 6 chữ số")
    private String newPin;
}
