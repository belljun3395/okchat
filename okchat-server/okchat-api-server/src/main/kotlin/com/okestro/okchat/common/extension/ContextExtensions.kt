package com.okestro.okchat.common.extension

import io.micrometer.context.ContextSnapshot
import reactor.util.context.Context
import reactor.util.context.ContextView

fun ContextView.copyToMdc(): ContextSnapshot.Scope {
    return ContextSnapshot.captureAll(this).setThreadLocals()
}

fun Context.copyToMdc(): ContextSnapshot.Scope {
    return ContextSnapshot.captureAll(this).setThreadLocals()
}
