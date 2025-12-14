package com.okestro.okchat.search.util

fun String.extractChunk(): String {
    return if (this.contains("_chunk_")) {
        this.substringBefore("_chunk_")
    } else {
        this
    }
}

object DocumentUtils
