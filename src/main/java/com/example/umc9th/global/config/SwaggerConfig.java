package com.example.umc9th.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // JWT 토큰 헤더 방식 설정
        String securitySchemeName = "JWT TOKEN";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);

        Components components = new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("Bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    private Info apiInfo() {
        return new Info()
                .title("UMC 9th Spring Boot API - 김덕환")
                .description("UMC 9기 Spring Boot API - 김덕환 작성")
                .version("1.0.0");
    }

    private List<Server> servers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("로컬 개발 서버"),
                new Server()
                        .url("https://spring-swagger-api.log8.kr")
                        .description("배포 서버 (Mac Mini)")
        );
    }
}
