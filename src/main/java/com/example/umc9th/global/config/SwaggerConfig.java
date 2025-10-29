package com.example.umc9th.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers());
    }

    private Info apiInfo() {
        return new Info()
                .title("UMC 9th Spring Boot API")
                .description("UMC 9기 Spring Boot + JPA 학습 프로젝트 API 명세서")
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
