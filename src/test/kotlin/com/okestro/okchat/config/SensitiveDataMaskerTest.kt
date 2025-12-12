package com.okestro.okchat.config

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SensitiveDataMasker Tests")
class SensitiveDataMaskerTest {

    @Test
    fun `should mask password-like fields`() {
        SensitiveDataMasker.mask("""{"password":"MySecret123"}""") shouldBe
            """{"password":"***MASKED***"}"""
    }

    @Test
    fun `should mask api key fields`() {
        SensitiveDataMasker.mask("""{"apiKey":"sk-123"}""") shouldBe
            """{"apiKey":"***MASKED***"}"""
    }

    @Test
    fun `should mask bearer tokens`() {
        SensitiveDataMasker.mask("Authorization: Bearer abc.def.ghi") shouldBe
            "Authorization: Bearer ***MASKED***"
    }

    @Test
    fun `should mask email addresses`() {
        SensitiveDataMasker.mask("user@example.com") shouldBe
            "us**@example.com"
    }

    @Test
    fun `should mask credit card numbers`() {
        SensitiveDataMasker.mask("1234-5678-9012-3456") shouldBe
            "**** **** **** 3456"
    }
}
