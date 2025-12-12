package com.okestro.okchat.config

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent

/**
 * Masks sensitive data in pattern-based appenders (CONSOLE/FILE).
 *
 * Used via conversion rule `maskedMsg` in logback-spring.xml.
 */
class MaskedMessageConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent): String {
        return SensitiveDataMasker.mask(event.formattedMessage).orEmpty()
    }
}
