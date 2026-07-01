package com.waterai.consultant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI waterAiConsultantOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI 水务项目智能顾问平台 API")
                        .version("v0.1.0")
                        .description("项目初始化阶段 API 文档"));
    }
}

