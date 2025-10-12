package com.okestro.okchat.chat.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Mono

@DisplayName("SessionManagementService Unit Tests")
class SessionManagementServiceTest {

    private lateinit var redisTemplate: ReactiveRedisTemplate<String, String>
    private lateinit var objectMapper: ObjectMapper
    private lateinit var service: SessionManagementService
    private lateinit var valueOps: ReactiveValueOperations<String, String>

    @BeforeEach
    fun setUp() {
        redisTemplate = mockk()
        objectMapper = jacksonObjectMapper()
        valueOps = mockk()
        service = SessionManagementService(redisTemplate, objectMapper)

        every { redisTemplate.opsForValue() } returns valueOps
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    @DisplayName("generateSessionId should generate valid UUID session ID")
    fun `should generate valid session ID`() {
        // when
        val sessionId = service.generateSessionId()

        // then
        sessionId.shouldNotBeNull()
        sessionId shouldMatch Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }

    @Test
    @DisplayName("loadConversationHistory should return null when session not found")
    fun `should return null when session not found`() = runTest {
        // given
        val sessionId = "non-existent-session"
        every { valueOps.get(any()) } returns Mono.empty()

        // when
        val result = service.loadConversationHistory(sessionId)

        // then
        result shouldBe null
    }

    @Test
    @DisplayName("clearSession should clear session successfully")
    fun `should clear session successfully`() = runTest {
        // given
        val sessionId = "test-session"
        every { redisTemplate.delete("chat:session:$sessionId") } returns Mono.just(1L)

        // when
        val result = service.clearSession(sessionId)

        // then
        result shouldBe true
        verify(exactly = 1) { redisTemplate.delete("chat:session:$sessionId") }
    }

    @Test
    @DisplayName("clearSession should return false when session not found")
    fun `should return false when session not found for clearing`() = runTest {
        // given
        val sessionId = "test-session"
        every { redisTemplate.delete("chat:session:$sessionId") } returns Mono.just(0L)

        // when
        val result = service.clearSession(sessionId)

        // then
        result shouldBe false
    }

    @Test
    @DisplayName("clearSession should return false when error occurs")
    fun `should return false when error occurs during clearing`() = runTest {
        // given
        val sessionId = "test-session"
        every { redisTemplate.delete("chat:session:$sessionId") } returns Mono.error(RuntimeException("Redis error"))

        // when
        val result = service.clearSession(sessionId)

        // then
        result shouldBe false
    }

    @Test
    @DisplayName("generateSessionId should generate different IDs each time")
    fun `should generate different session IDs`() {
        // when
        val id1 = service.generateSessionId()
        val id2 = service.generateSessionId()
        val id3 = service.generateSessionId()

        // then
        id1 shouldMatch Regex("[0-9a-f-]{36}")
        id2 shouldMatch Regex("[0-9a-f-]{36}")
        id3 shouldMatch Regex("[0-9a-f-]{36}")
        // All IDs should be different
        (id1 != id2) shouldBe true
        (id2 != id3) shouldBe true
        (id1 != id3) shouldBe true
    }
}
