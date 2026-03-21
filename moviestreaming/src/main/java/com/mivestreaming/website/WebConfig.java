package com.mivestreaming.website;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Web – đăng ký JwtAuthFilter để chạy trên mọi request.
 *
 * JwtAuthFilter không block request, chỉ đọc token (nếu có) và gắn
 * thông tin vào request attribute. Việc kiểm tra quyền do AdminGuard đảm nhiệm.
 */
@Configuration
public class WebConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public WebConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Đăng ký JwtAuthFilter với ưu tiên cao nhất (order = 1)
     * để chạy trước tất cả filter khác.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(jwtAuthFilter);
        reg.addUrlPatterns("/api/*"); // Chỉ áp dụng cho các URL /api/...
        reg.setOrder(1);
        return reg;
    }
}
