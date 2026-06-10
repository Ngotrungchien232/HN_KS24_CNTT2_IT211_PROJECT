package com.project_cuoimon.service;

import com.project_cuoimon.dto.AccountResponse;
import com.project_cuoimon.entity.User;

public interface AccountService {
    // Vấn tin số dư tài khoản của người dùng
    AccountResponse getBalance(User user);
}