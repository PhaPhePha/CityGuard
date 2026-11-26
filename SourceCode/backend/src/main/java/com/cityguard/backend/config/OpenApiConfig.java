package com.cityguard.backend.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình OpenAPI / Swagger tích hợp Bearer JWT Authorization.
 */
@Configuration
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer",
        description = "Nhập Access Token (JWT) được cấp sau khi đăng nhập (không cần gõ tiền tố 'Bearer ')"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CityGuard Backend API")
                        .version("1.0")
                        .description("Tài liệu API hệ thống CityGuard - Giám sát và báo cáo sự cố giao thông đô thị"))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}

