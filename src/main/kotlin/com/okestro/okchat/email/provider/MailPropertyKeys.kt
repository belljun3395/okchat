package com.okestro.okchat.email.provider

/**
 * Constants for JavaMail properties
 */
object MailPropertyKeys {
    const val STORE_PROTOCOL = "mail.store.protocol"
    const val DEBUG = "mail.debug"
    const val DEBUG_AUTH = "mail.debug.auth"

    fun host(protocol: String) = "mail.$protocol.host"
    fun port(protocol: String) = "mail.$protocol.port"
    fun timeout(protocol: String) = "mail.$protocol.timeout"
    fun connectionTimeout(protocol: String) = "mail.$protocol.connectiontimeout"
    fun sslEnable(protocol: String) = "mail.$protocol.ssl.enable"
    fun sslTrust(protocol: String) = "mail.$protocol.ssl.trust"
    fun auth(protocol: String) = "mail.$protocol.auth"
    fun authMechanisms(protocol: String) = "mail.$protocol.auth.mechanisms"
    fun authLoginDisable(protocol: String) = "mail.$protocol.auth.login.disable"
    fun authPlainDisable(protocol: String) = "mail.$protocol.auth.plain.disable"
    fun saslEnable(protocol: String) = "mail.$protocol.sasl.enable"
    fun saslMechanisms(protocol: String) = "mail.$protocol.sasl.mechanisms"
}
