package com.okestro.okchat.confluence.model

data class ContentNode(
    val id: String,
    val title: String,
    val type: ContentType,
    val parentId: String? = null,
    val body: String? = null,
    val children: MutableList<ContentNode> = mutableListOf()
) {
    fun hasChildren(): Boolean = children.isNotEmpty()

    fun getChildCount(): Int = children.size

    fun getDescendantCount(): Int {
        var count = children.size
        children.forEach { count += it.getDescendantCount() }
        return count
    }

    override fun toString(): String {
        val icon = if (type == ContentType.FOLDER) "📁" else "📄"
        val childInfo = if (children.isNotEmpty()) " (${children.size})" else ""
        return "$icon $title$childInfo [ID: $id]"
    }
}
