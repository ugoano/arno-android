package network.arno.android.command

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the pure logic extracted from accessibility command handlers.
 * These test the mapping/parsing/formatting functions that don't require
 * Android framework classes.
 */
class AccessibilityCommandsTest {

    // ── NavigateAction mapping tests ──

    @Test
    fun `NavigateAction fromString maps back correctly`() {
        val action = NavigateAction.fromString("back")
        assertNotNull(action)
        assertEquals(NavigateAction.BACK, action)
    }

    @Test
    fun `NavigateAction fromString maps home correctly`() {
        val action = NavigateAction.fromString("home")
        assertNotNull(action)
        assertEquals(NavigateAction.HOME, action)
    }

    @Test
    fun `NavigateAction fromString maps recents correctly`() {
        val action = NavigateAction.fromString("recents")
        assertNotNull(action)
        assertEquals(NavigateAction.RECENTS, action)
    }

    @Test
    fun `NavigateAction fromString is case insensitive`() {
        assertEquals(NavigateAction.BACK, NavigateAction.fromString("BACK"))
        assertEquals(NavigateAction.HOME, NavigateAction.fromString("Home"))
        assertEquals(NavigateAction.RECENTS, NavigateAction.fromString("RECENTS"))
    }

    @Test
    fun `NavigateAction fromString returns null for unknown action`() {
        assertNull(NavigateAction.fromString("invalid"))
        assertNull(NavigateAction.fromString(""))
    }

    @Test
    fun `NavigateAction has correct global action IDs`() {
        // AccessibilityService.GLOBAL_ACTION_BACK = 1
        assertEquals(1, NavigateAction.BACK.globalActionId)
        // AccessibilityService.GLOBAL_ACTION_HOME = 2
        assertEquals(2, NavigateAction.HOME.globalActionId)
        // AccessibilityService.GLOBAL_ACTION_RECENTS = 3
        assertEquals(3, NavigateAction.RECENTS.globalActionId)
    }

    // ── ScrollDirection mapping tests ──

    @Test
    fun `ScrollDirection fromString maps up correctly`() {
        assertEquals(ScrollDirection.UP, ScrollDirection.fromString("up"))
    }

    @Test
    fun `ScrollDirection fromString maps down correctly`() {
        assertEquals(ScrollDirection.DOWN, ScrollDirection.fromString("down"))
    }

    @Test
    fun `ScrollDirection fromString maps left correctly`() {
        assertEquals(ScrollDirection.LEFT, ScrollDirection.fromString("left"))
    }

    @Test
    fun `ScrollDirection fromString maps right correctly`() {
        assertEquals(ScrollDirection.RIGHT, ScrollDirection.fromString("right"))
    }

    @Test
    fun `ScrollDirection fromString is case insensitive`() {
        assertEquals(ScrollDirection.UP, ScrollDirection.fromString("UP"))
        assertEquals(ScrollDirection.DOWN, ScrollDirection.fromString("Down"))
    }

    @Test
    fun `ScrollDirection fromString returns null for unknown direction`() {
        assertNull(ScrollDirection.fromString("diagonal"))
        assertNull(ScrollDirection.fromString(""))
    }

    // ── NodeTextExtractor tests ──

    @Test
    fun `NodeTextExtractor formatNode produces correct output for text node`() {
        val result = NodeTextExtractor.formatNode(
            depth = 0,
            className = "android.widget.TextView",
            text = "Hello World",
            contentDescription = null,
            isClickable = false,
            isScrollable = false,
            resourceId = "com.example:id/title",
        )
        assertEquals("[TextView] \"Hello World\" (id:title)", result)
    }

    @Test
    fun `NodeTextExtractor formatNode includes content description`() {
        val result = NodeTextExtractor.formatNode(
            depth = 1,
            className = "android.widget.ImageButton",
            text = null,
            contentDescription = "Settings",
            isClickable = true,
            isScrollable = false,
            resourceId = null,
        )
        assertEquals("  [ImageButton] desc:\"Settings\" [clickable]", result)
    }

    @Test
    fun `NodeTextExtractor formatNode marks clickable and scrollable`() {
        val result = NodeTextExtractor.formatNode(
            depth = 0,
            className = "android.widget.ScrollView",
            text = null,
            contentDescription = null,
            isClickable = true,
            isScrollable = true,
            resourceId = null,
        )
        assertEquals("[ScrollView] [clickable] [scrollable]", result)
    }

    @Test
    fun `NodeTextExtractor formatNode indents by depth`() {
        val result = NodeTextExtractor.formatNode(
            depth = 3,
            className = "android.widget.Button",
            text = "OK",
            contentDescription = null,
            isClickable = true,
            isScrollable = false,
            resourceId = null,
        )
        assertTrue(result.startsWith("      ")) // 3 * 2 spaces
        assertTrue(result.contains("[Button]"))
        assertTrue(result.contains("\"OK\""))
    }

    @Test
    fun `NodeTextExtractor formatNode extracts simple class name`() {
        val result = NodeTextExtractor.formatNode(
            depth = 0,
            className = "android.widget.TextView",
            text = "Test",
            contentDescription = null,
            isClickable = false,
            isScrollable = false,
            resourceId = null,
        )
        assertTrue(result.contains("[TextView]"))
        assertFalse(result.contains("android.widget"))
    }

    @Test
    fun `NodeTextExtractor formatNode handles null className`() {
        val result = NodeTextExtractor.formatNode(
            depth = 0,
            className = null,
            text = "Test",
            contentDescription = null,
            isClickable = false,
            isScrollable = false,
            resourceId = null,
        )
        assertTrue(result.contains("[View]"))
    }

    @Test
    fun `NodeTextExtractor formatNode extracts short resource id`() {
        val result = NodeTextExtractor.formatNode(
            depth = 0,
            className = "android.widget.EditText",
            text = null,
            contentDescription = null,
            isClickable = false,
            isScrollable = false,
            resourceId = "com.example.app:id/email_input",
        )
        assertTrue(result.contains("(id:email_input)"))
        assertFalse(result.contains("com.example.app"))
    }

    @Test
    fun `NodeTextExtractor formatNode skips empty attributes`() {
        val result = NodeTextExtractor.formatNode(
            depth = 0,
            className = "android.view.View",
            text = null,
            contentDescription = null,
            isClickable = false,
            isScrollable = false,
            resourceId = null,
        )
        assertEquals("[View]", result)
    }

    // ── ElementMatcher tests ──

    @Test
    fun `ElementMatcher matches by exact text`() {
        assertTrue(
            ElementMatcher.matches(
                nodeText = "Submit",
                nodeContentDescription = null,
                targetText = "Submit",
                targetContentDescription = null,
            )
        )
    }

    @Test
    fun `ElementMatcher matches by text case insensitive`() {
        assertTrue(
            ElementMatcher.matches(
                nodeText = "Submit",
                nodeContentDescription = null,
                targetText = "submit",
                targetContentDescription = null,
            )
        )
    }

    @Test
    fun `ElementMatcher matches by content description`() {
        assertTrue(
            ElementMatcher.matches(
                nodeText = null,
                nodeContentDescription = "Close button",
                targetText = null,
                targetContentDescription = "Close button",
            )
        )
    }

    @Test
    fun `ElementMatcher matches content description case insensitive`() {
        assertTrue(
            ElementMatcher.matches(
                nodeText = null,
                nodeContentDescription = "Settings Menu",
                targetText = null,
                targetContentDescription = "settings menu",
            )
        )
    }

    @Test
    fun `ElementMatcher matches by partial text contains`() {
        assertTrue(
            ElementMatcher.matches(
                nodeText = "Submit Order",
                nodeContentDescription = null,
                targetText = "Submit",
                targetContentDescription = null,
            )
        )
    }

    @Test
    fun `ElementMatcher matches by partial content description contains`() {
        assertTrue(
            ElementMatcher.matches(
                nodeText = null,
                nodeContentDescription = "Navigate back to home",
                targetText = null,
                targetContentDescription = "Navigate back",
            )
        )
    }

    @Test
    fun `ElementMatcher prefers text match when both specified`() {
        assertTrue(
            ElementMatcher.matches(
                nodeText = "Submit",
                nodeContentDescription = "Wrong",
                targetText = "Submit",
                targetContentDescription = null,
            )
        )
    }

    @Test
    fun `ElementMatcher returns false when nothing matches`() {
        assertFalse(
            ElementMatcher.matches(
                nodeText = "Cancel",
                nodeContentDescription = "Cancel button",
                targetText = "Submit",
                targetContentDescription = "Submit button",
            )
        )
    }

    @Test
    fun `ElementMatcher returns false when node has no text and no description`() {
        assertFalse(
            ElementMatcher.matches(
                nodeText = null,
                nodeContentDescription = null,
                targetText = "Submit",
                targetContentDescription = null,
            )
        )
    }

    @Test
    fun `ElementMatcher returns false when target is empty`() {
        assertFalse(
            ElementMatcher.matches(
                nodeText = "Submit",
                nodeContentDescription = null,
                targetText = null,
                targetContentDescription = null,
            )
        )
    }
}
