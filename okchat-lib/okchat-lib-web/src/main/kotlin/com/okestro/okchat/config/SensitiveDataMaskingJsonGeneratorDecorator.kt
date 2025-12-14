package com.okestro.okchat.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate
import net.logstash.logback.decorate.JsonGeneratorDecorator
import kotlin.math.min

/**
 * JsonGeneratorDecorator that masks sensitive data in all string fields,
 * including the `message` field of LogstashEncoder.
 *
 * Applied via logback-spring.xml:
 * <jsonGeneratorDecorator class="com.okestro.okchat.config.SensitiveDataMaskingJsonGeneratorDecorator"/>
 */
class SensitiveDataMaskingJsonGeneratorDecorator : JsonGeneratorDecorator {
    override fun decorate(generator: JsonGenerator): JsonGenerator {
        return object : JsonGeneratorDelegate(generator) {
            override fun writeString(text: String?) {
                super.writeString(SensitiveDataMasker.mask(text))
            }

            override fun writeStringField(fieldName: String?, value: String?) {
                super.writeStringField(fieldName, SensitiveDataMasker.mask(value))
            }
        }
    }
}

/**
 * Central masking logic. Keep this pure and cheap.
 */
internal object SensitiveDataMasker {
    private const val FULL_MASK = "***MASKED***"

    private val passwordPattern =
        Regex("(?i)(\"?password\"?\\s*[:=]\\s*\")([^\"]+)(\")")

    private val apiKeyPattern =
        Regex("(?i)(\"?(?:api[-_]?key|token|secret)\"?\\s*[:=]\\s*\")([^\"]+)(\")")

    private val bearerTokenPattern =
        Regex("(?i)Bearer\\s+[A-Za-z0-9\\-_.=+/]+")

    private val emailPattern =
        Regex("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})")

    private val creditCardPattern =
        Regex("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b")

    fun mask(input: String?): String? {
        if (input.isNullOrBlank()) return input
        var masked = input

        // 1) JSON-like password/apiKey fields
        masked = masked.replace(passwordPattern) { m ->
            "${m.groupValues[1]}$FULL_MASK${m.groupValues[3]}"
        }
        masked = masked.replace(apiKeyPattern) { m ->
            "${m.groupValues[1]}$FULL_MASK${m.groupValues[3]}"
        }

        // 2) Bearer tokens
        masked = masked.replace(bearerTokenPattern) { "Bearer $FULL_MASK" }

        // 3) Emails (keep first 2 chars of local part)
        masked = masked.replace(emailPattern) { m ->
            val local = m.groupValues[1]
            val domain = m.groupValues[2]
            val keep = min(2, local.length)
            val visible = local.take(keep)
            val stars = "*".repeat((local.length - keep).coerceAtLeast(1))
            "$visible$stars@$domain"
        }

        // 4) Credit cards (keep last 4 digits)
        masked = masked.replace(creditCardPattern) { m ->
            val digits = m.value.replace(" ", "").replace("-", "")
            val last4 = digits.takeLast(4)
            "**** **** **** $last4"
        }

        return masked
    }
}
