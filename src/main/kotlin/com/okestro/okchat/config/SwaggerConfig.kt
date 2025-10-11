package com.okestro.okchat.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("OkChat API Documentation")
                    .version("v1.0.0")
                    .description(
                        """
                        OkChat API 문서입니다. 
                        
                        ## 주요 기능
                        - **Chat**: AI 기반 채팅 및 문서 검색
                        - **Email**: 이메일 관리 및 OAuth2 인증
                        - **Permission**: 문서 권한 관리
                        - **Prompt**: 프롬프트 버전 관리
                        - **User**: 사용자 관리
                        - **Task**: 백그라운드 작업 모니터링
                        
                        각 기능별로 그룹화된 문서를 좌측 상단의 드롭다운에서 선택하여 확인할 수 있습니다.
                        """.trimIndent()
                    )
            )
            .servers(
                listOf(
                    Server().url("/").description("Current Server")
                )
            )
    }

    @Bean
    fun chatApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("01. Chat API")
            .displayName("Chat API")
            .pathsToMatch("/api/chat/**")
            .packagesToScan("com.okestro.okchat.chat.controller")
            .build()
    }

    @Bean
    fun emailApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("02. Email API")
            .displayName("Email API")
            .pathsToMatch("/api/email/**", "/oauth2/**")
            .packagesToScan("com.okestro.okchat.email.controller", "com.okestro.okchat.email.oauth2")
            .build()
    }

    @Bean
    fun permissionApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("03. Permission API")
            .displayName("Permission API")
            .pathsToMatch("/api/admin/permissions/**")
            .packagesToScan("com.okestro.okchat.permission.controller")
            .build()
    }

    @Bean
    fun promptApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("04. Prompt API")
            .displayName("Prompt API")
            .pathsToMatch("/api/prompts/**")
            .packagesToScan("com.okestro.okchat.prompt.controller")
            .build()
    }

    @Bean
    fun userApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("05. User API")
            .displayName("User API")
            .pathsToMatch("/api/admin/users/**")
            .packagesToScan("com.okestro.okchat.user.controller")
            .build()
    }

    @Bean
    fun taskApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("06. Task API")
            .displayName("Task API")
            .pathsToMatch("/api/tasks/**")
            .packagesToScan("com.okestro.okchat.task")
            .build()
    }

    @Bean
    fun webApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("99. Web Pages")
            .displayName("Web Pages")
            .pathsToMatch("/admin/**")
            .build()
    }

    @Bean
    fun allApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("00. All APIs")
            .displayName("All APIs")
            .pathsToMatch("/api/**", "/oauth2/**", "/admin/**")
            .build()
    }
}
