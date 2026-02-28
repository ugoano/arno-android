package network.arno.android.command

/**
 * Pure logic for formatting accessibility node information as human-readable text.
 * Extracted from ArnoAccessibilityService for testability.
 *
 * Format: [ClassName] "text" desc:"description" (id:shortId) [clickable] [scrollable]
 * Indented by depth (2 spaces per level).
 */
object NodeTextExtractor {

    fun formatNode(
        depth: Int,
        className: String?,
        text: String?,
        contentDescription: String?,
        isClickable: Boolean,
        isScrollable: Boolean,
        resourceId: String?,
    ): String {
        val indent = "  ".repeat(depth)
        val parts = mutableListOf<String>()

        // Short class name (e.g. "TextView" from "android.widget.TextView")
        val shortName = className?.substringAfterLast('.') ?: "View"
        parts.add("[$shortName]")

        // Text content
        if (!text.isNullOrEmpty()) {
            parts.add("\"$text\"")
        }

        // Content description
        if (!contentDescription.isNullOrEmpty()) {
            parts.add("desc:\"$contentDescription\"")
        }

        // Resource ID (short form)
        if (!resourceId.isNullOrEmpty()) {
            val shortId = resourceId.substringAfter(":id/", resourceId)
            parts.add("(id:$shortId)")
        }

        // Interaction flags
        if (isClickable) parts.add("[clickable]")
        if (isScrollable) parts.add("[scrollable]")

        return indent + parts.joinToString(" ")
    }
}
