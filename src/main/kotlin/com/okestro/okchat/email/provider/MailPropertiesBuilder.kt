package com.okestro.okchat.email.provider

import com.okestro.okchat.email.config.EmailDefaults
import java.util.Properties

class MailPropertiesBuilder(private val protocol: String = EmailDefaults.PROTOCOL) {
    private val properties = Properties()

    fun basicConfig(host: String, port: Int, timeout: Int = 10000) = apply {
        properties.apply {
            put(MailPropertyKeys.STORE_PROTOCOL, protocol)
            put(MailPropertyKeys.host(protocol), host)
            put(MailPropertyKeys.port(protocol), port.toString())
            put(MailPropertyKeys.timeout(protocol), timeout.toString())
            put(MailPropertyKeys.connectionTimeout(protocol), timeout.toString())
        }
    }

    fun sslConfig(host: String) = apply {
        properties.apply {
            put(MailPropertyKeys.sslEnable(protocol), "true")
            put(MailPropertyKeys.sslTrust(protocol), host)
        }
    }

    fun oauth2Config() = apply {
        properties.apply {
            put(MailPropertyKeys.authMechanisms(protocol), "XOAUTH2")
            put(MailPropertyKeys.authLoginDisable(protocol), "true")
            put(MailPropertyKeys.authPlainDisable(protocol), "true")
            put(MailPropertyKeys.saslEnable(protocol), "true")
            put(MailPropertyKeys.saslMechanisms(protocol), "XOAUTH2")
        }
    }

    fun plainAuthConfig() = apply {
        properties.put(MailPropertyKeys.auth(protocol), "true")
    }

    fun debugConfig(enabled: Boolean = true) = apply {
        properties.apply {
            put(MailPropertyKeys.DEBUG, enabled.toString())
            put(MailPropertyKeys.DEBUG_AUTH, enabled.toString())
        }
    }

    fun customProperty(key: String, value: String) = apply {
        properties.put(key, value)
    }

    fun build(): Properties = properties
}

fun buildMailProperties(protocol: String = EmailDefaults.PROTOCOL, builder: MailPropertiesBuilder.() -> Unit): Properties =
    MailPropertiesBuilder(protocol).apply(builder).build()
