package com.project_cuoimon.config;

import com.project_cuoimon.security.AuthEntryPointJwt;
import com.project_cuoimon.security.AuthTokenFilter;
import com.project_cuoimon.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    // 1. Tạo Bean cho bộ lọc Token của chúng ta
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    // 2. Cấu hình Provider kết nối UserDetailsService và bộ mã hóa mật khẩu
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        
        return authProvider;
    }

    // 3. Tạo Bean AuthenticationManager để phục vụ cho API Đăng nhập
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // 4. Định nghĩa thuật toán mã hóa mật khẩu BCrypt với độ mạnh 10 (Đáp ứng yêu cầu NFR-02)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // 5. Cấu hình chuỗi lọc bảo mật (Security Filter Chain) chính của hệ thống
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Vô hiệu hóa CSRF vì API Stateless dùng JWT không bị tấn công CSRF thông thường
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler)) // Gán bộ xử lý lỗi 401
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Thiết lập chế độ Stateless
            .authorizeHttpRequests(auth -> 
                auth.requestMatchers("/api/auth/**", "/error").permitAll() // Các API xác thực và đường dẫn lỗi được phép truy cập công khai
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN") // Chỉ ADMIN mới vào được
                    .requestMatchers("/api/v1/staff/**").hasRole("STAFF") // Chỉ STAFF mới vào được
                    .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER") // Chỉ CUSTOMER mới vào được
                    .anyRequest().authenticated() // Tất cả các yêu cầu khác đều phải xác thực (đóng lỗ hổng anyRequest().permitAll())
            );

        // Đặt AuthenticationProvider đã cấu hình vào hệ thống
        http.authenticationProvider(authenticationProvider());

        // Đưa bộ lọc AuthTokenFilter của chúng ta vào TRƯỚC bộ lọc xác thực mặc định của Spring
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
