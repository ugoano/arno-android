package network.arno.android.notification

enum class FilterMode {
    WHITELIST,
    BLACKLIST,
}

data class NotificationFilter(
    val mode: FilterMode,
    val packages: Set<String>,
    val ownPackage: String? = null,
) {
    fun shouldCapture(packageName: String): Boolean {
        if (packageName == ownPackage) return false
        return when (mode) {
            FilterMode.WHITELIST -> packageName in packages
            FilterMode.BLACKLIST -> packageName !in packages
        }
    }

    fun toStringSet(): Set<String> = packages

    companion object {
        private const val DEFAULT_SLACK_PACKAGE = "com.Slack"

        fun default(): NotificationFilter = NotificationFilter(
            mode = FilterMode.WHITELIST,
            packages = setOf(DEFAULT_SLACK_PACKAGE),
        )

        fun fromStringSet(
            stringSet: Set<String>,
            mode: FilterMode,
            ownPackage: String? = null,
        ): NotificationFilter = NotificationFilter(
            mode = mode,
            packages = stringSet,
            ownPackage = ownPackage,
        )
    }
}
