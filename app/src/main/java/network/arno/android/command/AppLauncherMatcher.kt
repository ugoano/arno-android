package network.arno.android.command

/**
 * Represents an installed app with its display name and package name.
 * Used as the intermediary between PackageManager queries and matching logic.
 */
data class AppInfo(val name: String, val packageName: String)

/**
 * Result of an app name match attempt.
 */
sealed class MatchResult {
    /** App found — includes the matched [app] info. */
    data class Found(val app: AppInfo) : MatchResult()

    /** No match — includes up to 5 [suggestions] of installed app names. */
    data class NotFound(val suggestions: List<String>) : MatchResult()
}

/**
 * Pure matching logic for resolving user-provided app names to installed apps.
 * Extracted from AppLauncherHandler for testability (no Android dependencies).
 *
 * Match priority:
 * 1. Exact match (case-insensitive)
 * 2. Prefix match (query is prefix of app name, case-insensitive)
 * 3. Substring match (query appears anywhere in app name, case-insensitive)
 *
 * If no match is found, returns up to 5 suggestions from the installed app list.
 */
object AppLauncherMatcher {

    private const val MAX_SUGGESTIONS = 5

    fun findMatch(query: String, apps: List<AppInfo>): MatchResult {
        if (query.isBlank() || apps.isEmpty()) {
            return MatchResult.NotFound(
                suggestions = apps.take(MAX_SUGGESTIONS).map { it.name },
            )
        }

        val queryLower = query.lowercase()

        // Priority 1: Exact match (case-insensitive)
        val exact = apps.firstOrNull { it.name.lowercase() == queryLower }
        if (exact != null) return MatchResult.Found(exact)

        // Priority 2: Prefix match
        val prefix = apps.firstOrNull { it.name.lowercase().startsWith(queryLower) }
        if (prefix != null) return MatchResult.Found(prefix)

        // Priority 3: Substring match
        val substring = apps.firstOrNull { it.name.lowercase().contains(queryLower) }
        if (substring != null) return MatchResult.Found(substring)

        // No match — provide suggestions (alphabetically sorted, limited)
        val suggestions = apps
            .map { it.name }
            .sorted()
            .take(MAX_SUGGESTIONS)

        return MatchResult.NotFound(suggestions)
    }
}
