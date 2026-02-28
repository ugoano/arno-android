package network.arno.android.command

/**
 * Pure logic for matching UI elements by text or content description.
 * Extracted from AccessibilityService for testability.
 *
 * Supports exact and partial (contains) matching, case insensitive.
 */
object ElementMatcher {

    /**
     * Check if a node matches the target criteria.
     *
     * @param nodeText The node's text content
     * @param nodeContentDescription The node's content description
     * @param targetText The text to match against (from command payload)
     * @param targetContentDescription The content description to match against
     * @return true if the node matches any of the target criteria
     */
    fun matches(
        nodeText: String?,
        nodeContentDescription: String?,
        targetText: String?,
        targetContentDescription: String?,
    ): Boolean {
        // Must have at least one target criterion
        if (targetText.isNullOrEmpty() && targetContentDescription.isNullOrEmpty()) {
            return false
        }

        // Try text match first
        if (!targetText.isNullOrEmpty() && !nodeText.isNullOrEmpty()) {
            if (nodeText.contains(targetText, ignoreCase = true)) {
                return true
            }
        }

        // Try content description match
        if (!targetContentDescription.isNullOrEmpty() && !nodeContentDescription.isNullOrEmpty()) {
            if (nodeContentDescription.contains(targetContentDescription, ignoreCase = true)) {
                return true
            }
        }

        return false
    }
}
