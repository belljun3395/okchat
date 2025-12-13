
package com.okestro.okchat.chat.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.MDC
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/chat")
@Tag(
    name = "Chat API",
    description = "AI 기반 채팅 및 문서 검색 API. RAG(Retrieval-Augmented Generation)를 활용하여 문서 기반 답변을 제공합니다."
)
class ChatController(
    private val streamChatUseCase: com.okestro.okchat.chat.application.StreamChatUseCase
) {

    @PostMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(
        summary = "AI 채팅 (스트리밍)",
        description = """
            문서 기반 AI 채팅을 수행합니다. 
            
            ## 주요 기능
            - **Server-Sent Events (SSE)**: 실시간 스트리밍 응답
            - **RAG 검색**: 관련 문서를 검색하여 답변 생성
            - **세션 관리**: sessionId를 통한 대화 컨텍스트 유지
            - **Deep Think 모드**: 더 깊이 있는 분석 수행
            - **키워드 필터링**: 특정 키워드로 검색 범위 제한
            
            ## 응답 형식
            응답은 Server-Sent Events 형식으로 스트리밍됩니다.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "스트리밍 응답 성공",
                content = [Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (필수 파라미터 누락 등)"
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류"
            )
        ]
    )
    suspend fun chat(
        @Parameter(
            description = "채팅 요청 정보",
            required = true,
            content = [
                Content(
                    schema = Schema(implementation = ChatRequest::class),
                    examples = [
                        ExampleObject(
                            name = "기본 채팅",
                            value = """{"message": "사용자 권한 관리는 어떻게 하나요?"}"""
                        ),
                        ExampleObject(
                            name = "Deep Think 모드",
                            value = """{"message": "시스템 아키텍처를 설명해주세요", "isDeepThink": true}"""
                        ),
                        ExampleObject(
                            name = "키워드 필터링",
                            value = """{"message": "권한 관련 설정", "keywords": ["permission", "access"]}"""
                        ),
                        ExampleObject(
                            name = "세션 유지",
                            value = """{"message": "이전 답변에 대해 더 자세히 설명해주세요", "sessionId": "session-123"}"""
                        )
                    ]
                )
            ]
        )
        @RequestBody
        chatRequest: ChatRequest
    ): Flow<String> {
        // Get requestId from MDC
        val requestId = MDC.get("requestId") ?: UUID.randomUUID().toString()

        return flow {
            emit("__REQUEST_ID__:$requestId\n")

            streamChatUseCase.execute(
                com.okestro.okchat.chat.application.dto.StreamChatUseCaseIn(
                    message = chatRequest.message,
                    isDeepThink = chatRequest.isDeepThink,
                    keywords = chatRequest.keywords ?: emptyList(),
                    sessionId = chatRequest.sessionId,
                    userEmail = chatRequest.userEmail
                )
            ).asFlow().collect { chunk ->
                emit(chunk)
            }
        }
    }
}

@Schema(description = "채팅 요청 데이터")
data class ChatRequest(
    @field:Schema(
        description = "사용자 메시지",
        example = "사용자 권한 관리는 어떻게 하나요?",
        required = true
    )
    val message: String,

    @field:Schema(
        description = "검색 키워드 목록 (선택사항). 특정 키워드로 검색 범위를 제한합니다.",
        example = "[\"permission\", \"access\"]",
        required = false
    )
    val keywords: List<String>? = null,

    @field:Schema(
        description = "세션 ID (선택사항). 대화 컨텍스트를 유지하기 위한 식별자입니다.",
        example = "session-123",
        required = false
    )
    val sessionId: String? = null,

    @field:Schema(
        description = "Deep Think 모드 활성화 여부. true일 경우 더 깊이 있는 분석을 수행합니다.",
        example = "false",
        defaultValue = "false",
        required = false
    )
    val isDeepThink: Boolean = false,

    @field:Schema(
        description = "사용자 이메일 (선택사항). 권한 필터링 및 분석에 활용됩니다.",
        example = "user@example.com",
        required = false
    )
    val userEmail: String? = null
)
