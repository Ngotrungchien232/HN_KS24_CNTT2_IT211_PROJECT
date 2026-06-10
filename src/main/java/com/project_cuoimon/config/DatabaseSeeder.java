package com.project_cuoimon.config;

import com.project_cuoimon.entity.Role;
import com.project_cuoimon.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Tự động khởi tạo các vai trò mặc định vào database lúc ứng dụng khởi chạy
        createRoleIfNotExist("ROLE_ADMIN", "Quản trị viên hệ thống");
        createRoleIfNotExist("ROLE_STAFF", "Nhân viên giao dịch ngân hàng");
        createRoleIfNotExist("ROLE_CUSTOMER", "Khách hàng sử dụng dịch vụ");
    }

    private void createRoleIfNotExist(String name, String description) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = Role.builder()
                    .name(name)
                    .description(description)
                    .build();
            roleRepository.save(role);
        }
    }
}

