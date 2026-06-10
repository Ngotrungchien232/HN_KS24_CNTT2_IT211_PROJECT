package com.project_cuoimon.service;

import com.project_cuoimon.entity.Account;
import com.project_cuoimon.entity.User;
import com.project_cuoimon.dto.AccountResponse;
import com.project_cuoimon.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public AccountResponse getBalance(User user) {
        // Tìm kiếm tài khoản liên kết với User đang đăng nhập
        Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Tài khoản ngân hàng chưa được mở hoặc eKYC chưa được phê duyệt!"));

        if (!account.getActive()) {
            throw new RuntimeException("Tài khoản ngân hàng của bạn đang bị khóa!");
        }

        // Ánh xạ dữ liệu sang DTO trả về cho Client
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .active(account.getActive())
                .build();
    }
}