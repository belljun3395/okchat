package com.okestro.okchat.knowledge.model.value

object ContentPath {
    const val SEPARATOR = " > "

    fun of(nodes: List<String>): String {
        return nodes.joinToString(SEPARATOR)
    }

    fun split(path: String): List<String> {
        return path.split(SEPARATOR)
    }

    fun getParent(path: String): String? {
        val parts = split(path)
        return if (parts.size > 1) {
            parts.dropLast(1).joinToString(SEPARATOR)
        } else {
            null
        }
    }
}
