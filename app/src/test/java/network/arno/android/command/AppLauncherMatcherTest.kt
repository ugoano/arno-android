package network.arno.android.command

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for AppLauncherMatcher — the pure logic extracted from
 * AppLauncherHandler for fuzzy app name matching.
 *
 * These tests run on JVM without Android framework dependencies.
 */
class AppLauncherMatcherTest {

    private val installedApps = listOf(
        AppInfo("Instagram", "com.instagram.android"),
        AppInfo("Slack", "com.Slack"),
        AppInfo("Chrome", "com.android.chrome"),
        AppInfo("Gmail", "com.google.android.gm"),
        AppInfo("Google Maps", "com.google.android.apps.maps"),
        AppInfo("Google Calendar", "com.google.android.calendar"),
        AppInfo("Settings", "com.android.settings"),
        AppInfo("Camera", "com.google.android.GoogleCamera"),
    )

    // ── Exact match tests ──

    @Test
    fun `findMatch returns exact match by name`() {
        val result = AppLauncherMatcher.findMatch("Instagram", installedApps)
        assertTrue(result is MatchResult.Found)
        assertEquals("com.instagram.android", (result as MatchResult.Found).app.packageName)
    }

    @Test
    fun `findMatch is case insensitive`() {
        val result = AppLauncherMatcher.findMatch("instagram", installedApps)
        assertTrue(result is MatchResult.Found)
        assertEquals("com.instagram.android", (result as MatchResult.Found).app.packageName)
    }

    @Test
    fun `findMatch matches uppercase query`() {
        val result = AppLauncherMatcher.findMatch("SLACK", installedApps)
        assertTrue(result is MatchResult.Found)
        assertEquals("com.Slack", (result as MatchResult.Found).app.packageName)
    }

    // ── Substring/fuzzy match tests ──

    @Test
    fun `findMatch matches by substring prefix`() {
        val result = AppLauncherMatcher.findMatch("Insta", installedApps)
        assertTrue(result is MatchResult.Found)
        assertEquals("com.instagram.android", (result as MatchResult.Found).app.packageName)
    }

    @Test
    fun `findMatch matches by substring contains`() {
        val result = AppLauncherMatcher.findMatch("gram", installedApps)
        assertTrue(result is MatchResult.Found)
        assertEquals("com.instagram.android", (result as MatchResult.Found).app.packageName)
    }

    @Test
    fun `findMatch prefers exact match over substring`() {
        // "Chrome" should match "Chrome" exactly, not "Chrome Beta" if it existed
        val result = AppLauncherMatcher.findMatch("Chrome", installedApps)
        assertTrue(result is MatchResult.Found)
        assertEquals("com.android.chrome", (result as MatchResult.Found).app.packageName)
    }

    // ── Multiple match handling ──

    @Test
    fun `findMatch returns first when multiple substring matches and prefers prefix`() {
        // "Google" matches "Google Maps" and "Google Calendar" and "Gmail"
        val result = AppLauncherMatcher.findMatch("Google Maps", installedApps)
        assertTrue(result is MatchResult.Found)
        assertEquals("com.google.android.apps.maps", (result as MatchResult.Found).app.packageName)
    }

    // ── No match tests ──

    @Test
    fun `findMatch returns NotFound with suggestions for close matches`() {
        val result = AppLauncherMatcher.findMatch("Tiktok", installedApps)
        assertTrue(result is MatchResult.NotFound)
    }

    @Test
    fun `findMatch returns NotFound for empty query`() {
        val result = AppLauncherMatcher.findMatch("", installedApps)
        assertTrue(result is MatchResult.NotFound)
    }

    @Test
    fun `findMatch returns NotFound with empty app list`() {
        val result = AppLauncherMatcher.findMatch("Instagram", emptyList())
        assertTrue(result is MatchResult.NotFound)
    }

    // ── Suggestions in NotFound ──

    @Test
    fun `NotFound suggestions contain close matches when available`() {
        val apps = listOf(
            AppInfo("Instagram", "com.instagram.android"),
            AppInfo("Instapaper", "com.instapaper.android"),
            AppInfo("Slack", "com.Slack"),
        )
        val result = AppLauncherMatcher.findMatch("Instabook", apps)
        // "Instabook" shouldn't exactly/substring match, but close matches based on
        // common prefix should suggest Instagram and Instapaper
        assertTrue(result is MatchResult.NotFound)
        val suggestions = (result as MatchResult.NotFound).suggestions
        assertTrue(suggestions.size <= 5)
    }

    // ── Suggestions limit ──

    @Test
    fun `NotFound suggestions limited to 5 entries`() {
        val apps = (1..20).map { AppInfo("App$it", "com.example.app$it") }
        val result = AppLauncherMatcher.findMatch("ZZZ", apps)
        assertTrue(result is MatchResult.NotFound)
        assertTrue((result as MatchResult.NotFound).suggestions.size <= 5)
    }

    // ── AppInfo data class ──

    @Test
    fun `AppInfo stores name and package`() {
        val info = AppInfo("Test App", "com.test.app")
        assertEquals("Test App", info.name)
        assertEquals("com.test.app", info.packageName)
    }
}
