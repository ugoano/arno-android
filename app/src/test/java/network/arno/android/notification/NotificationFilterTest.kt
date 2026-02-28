package network.arno.android.notification

import org.junit.Assert.*
import org.junit.Test

class NotificationFilterTest {

    @Test
    fun `whitelist mode allows whitelisted package`() {
        val filter = NotificationFilter(
            mode = FilterMode.WHITELIST,
            packages = setOf("com.Slack"),
        )
        assertTrue(filter.shouldCapture("com.Slack"))
    }

    @Test
    fun `whitelist mode blocks non-whitelisted package`() {
        val filter = NotificationFilter(
            mode = FilterMode.WHITELIST,
            packages = setOf("com.Slack"),
        )
        assertFalse(filter.shouldCapture("com.whatsapp"))
    }

    @Test
    fun `whitelist mode with empty set blocks all`() {
        val filter = NotificationFilter(
            mode = FilterMode.WHITELIST,
            packages = emptySet(),
        )
        assertFalse(filter.shouldCapture("com.Slack"))
    }

    @Test
    fun `blacklist mode blocks blacklisted package`() {
        val filter = NotificationFilter(
            mode = FilterMode.BLACKLIST,
            packages = setOf("com.android.systemui"),
        )
        assertFalse(filter.shouldCapture("com.android.systemui"))
    }

    @Test
    fun `blacklist mode allows non-blacklisted package`() {
        val filter = NotificationFilter(
            mode = FilterMode.BLACKLIST,
            packages = setOf("com.android.systemui"),
        )
        assertTrue(filter.shouldCapture("com.Slack"))
    }

    @Test
    fun `blacklist mode with empty set allows all`() {
        val filter = NotificationFilter(
            mode = FilterMode.BLACKLIST,
            packages = emptySet(),
        )
        assertTrue(filter.shouldCapture("com.Slack"))
    }

    @Test
    fun `shouldCapture is case-sensitive for package names`() {
        val filter = NotificationFilter(
            mode = FilterMode.WHITELIST,
            packages = setOf("com.Slack"),
        )
        assertFalse(filter.shouldCapture("com.slack"))
    }

    @Test
    fun `default filter whitelists Slack only`() {
        val filter = NotificationFilter.default()
        assertTrue(filter.shouldCapture("com.Slack"))
        assertFalse(filter.shouldCapture("com.whatsapp"))
    }

    @Test
    fun `multiple packages in whitelist`() {
        val filter = NotificationFilter(
            mode = FilterMode.WHITELIST,
            packages = setOf("com.Slack", "com.microsoft.teams"),
        )
        assertTrue(filter.shouldCapture("com.Slack"))
        assertTrue(filter.shouldCapture("com.microsoft.teams"))
        assertFalse(filter.shouldCapture("com.whatsapp"))
    }

    @Test
    fun `own package is always filtered out`() {
        val filter = NotificationFilter(
            mode = FilterMode.BLACKLIST,
            packages = emptySet(),
            ownPackage = "network.arno.android",
        )
        assertFalse(filter.shouldCapture("network.arno.android"))
    }

    @Test
    fun `serialise whitelist to string set`() {
        val filter = NotificationFilter(
            mode = FilterMode.WHITELIST,
            packages = setOf("com.Slack", "com.microsoft.teams"),
        )
        val serialised = filter.toStringSet()
        assertTrue(serialised.contains("com.Slack"))
        assertTrue(serialised.contains("com.microsoft.teams"))
    }

    @Test
    fun `deserialise from string set`() {
        val stringSet = setOf("com.Slack", "com.microsoft.teams")
        val filter = NotificationFilter.fromStringSet(stringSet, FilterMode.WHITELIST)
        assertTrue(filter.shouldCapture("com.Slack"))
        assertTrue(filter.shouldCapture("com.microsoft.teams"))
        assertFalse(filter.shouldCapture("com.whatsapp"))
    }
}
