package network.arno.android.chat

/**
 * Pure comparison logic for local-first session rendering.
 *
 * When switching sessions, Room DB messages are shown instantly. The bridge
 * REST API history is fetched in background. This object determines whether
 * the API response differs from the locally displayed messages, avoiding
 * unnecessary UI updates (and potential flicker).
 */
object HistoryComparison {

    /**
     * Compare currently displayed messages with incoming bridge history.
     * Returns true if the UI should be updated with the incoming history.
     *
     * Only compares user and assistant messages (system/tool messages are
     * local-only and not present in bridge history).
     *
     * @param current The currently displayed ChatMessage list (from Room DB)
     * @param incoming The bridge REST API history as role-content pairs
     * @return true if incoming history differs and UI should update
     */
    fun shouldUpdateFromHistory(
        current: List<ChatMessage>,
        incoming: List<Pair<String, String>>,
    ): Boolean {
        // Empty incoming means bridge has no data - keep local
        if (incoming.isEmpty()) return false

        // Filter current to only user/assistant (bridge history won't have system/tool)
        val comparable = current.filter {
            it.role == ChatMessage.Role.USER || it.role == ChatMessage.Role.ASSISTANT
        }

        // Empty local but non-empty incoming - update needed
        if (comparable.isEmpty()) return true

        // Different count means different history
        if (comparable.size != incoming.size) return true

        // Compare role + content at each position
        for (i in comparable.indices) {
            val localMsg = comparable[i]
            val (incomingRole, incomingContent) = incoming[i]

            val localRoleName = localMsg.role.name.lowercase()
            if (localRoleName != incomingRole) return true
            if (localMsg.content != incomingContent) return true
        }

        return false
    }
}
