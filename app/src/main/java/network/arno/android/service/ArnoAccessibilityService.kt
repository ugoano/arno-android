package network.arno.android.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import network.arno.android.command.ElementMatcher
import network.arno.android.command.HandlerResult
import network.arno.android.command.NavigateAction
import network.arno.android.command.NodeTextExtractor
import network.arno.android.command.ScrollDirection

/**
 * AccessibilityService providing deep device control for Arno.
 *
 * Exposes screen reading, element tapping, text input, navigation,
 * and scrolling capabilities. Command handlers communicate with this
 * service via the singleton [instance] reference (same-process IPC).
 *
 * The user must enable this service manually in:
 * Settings > Accessibility > Arno
 */
class ArnoAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ArnoA11yService"
        private const val MAX_TREE_DEPTH = 20

        /**
         * Singleton reference to the running service instance.
         * Null when the service is not enabled or has been destroyed.
         * Command handlers check this to determine availability.
         */
        @Volatile
        var instance: ArnoAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service connected")

        serviceInfo = serviceInfo.apply {
            flags = flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Currently no proactive event handling.
        // Phase 5 will add screen change monitoring here.
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        instance = null
        Log.i(TAG, "Accessibility service destroyed")
        super.onDestroy()
    }

    // ── Public API for command handlers ──

    /**
     * Read the current screen's UI tree as structured text.
     */
    fun readScreen(): HandlerResult {
        val root = rootInActiveWindow
            ?: return HandlerResult.Error("No active window available (screen may be locked)")

        return try {
            val lines = mutableListOf<String>()
            traverseTree(root, 0, lines)
            root.recycle()

            val treeText = lines.joinToString("\n")
            val data = buildJsonObject {
                put("screen_content", treeText)
                put("node_count", lines.size)
            }
            HandlerResult.Success(data)
        } catch (e: Exception) {
            Log.e(TAG, "readScreen failed", e)
            HandlerResult.Error("readScreen failed: ${e.message}")
        }
    }

    /**
     * Find and tap a UI element by text or content description.
     */
    fun tapElement(text: String?, contentDescription: String?): HandlerResult {
        if (text.isNullOrEmpty() && contentDescription.isNullOrEmpty()) {
            return HandlerResult.Error("Must provide 'text' or 'content_description' to tap")
        }

        val root = rootInActiveWindow
            ?: return HandlerResult.Error("No active window available")

        return try {
            val target = findMatchingNode(root, text, contentDescription)
            if (target != null) {
                val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                val nodeDesc = target.text?.toString() ?: target.contentDescription?.toString() ?: "unknown"
                target.recycle()
                root.recycle()
                if (clicked) {
                    val data = buildJsonObject { put("tapped", nodeDesc) }
                    HandlerResult.Success(data)
                } else {
                    HandlerResult.Error("Element found but click action failed on: $nodeDesc")
                }
            } else {
                root.recycle()
                HandlerResult.Error("No element found matching text='$text' contentDescription='$contentDescription'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "tapElement failed", e)
            HandlerResult.Error("tapElement failed: ${e.message}")
        }
    }

    /**
     * Type text into the currently focused input field.
     */
    fun typeText(text: String): HandlerResult {
        val root = rootInActiveWindow
            ?: return HandlerResult.Error("No active window available")

        return try {
            val focused = findFocusedInput(root)
            if (focused != null) {
                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text,
                    )
                }
                val success = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                focused.recycle()
                root.recycle()
                if (success) {
                    val data = buildJsonObject { put("typed", text) }
                    HandlerResult.Success(data)
                } else {
                    HandlerResult.Error("Set text action failed on focused field")
                }
            } else {
                root.recycle()
                HandlerResult.Error("No focused input field found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "typeText failed", e)
            HandlerResult.Error("typeText failed: ${e.message}")
        }
    }

    /**
     * Perform a global navigation action (back, home, recents).
     */
    fun navigate(action: NavigateAction): HandlerResult {
        return try {
            val success = performGlobalAction(action.globalActionId)
            if (success) {
                val data = buildJsonObject { put("action", action.name.lowercase()) }
                HandlerResult.Success(data)
            } else {
                HandlerResult.Error("Global action ${action.name} failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "navigate failed", e)
            HandlerResult.Error("navigate failed: ${e.message}")
        }
    }

    /**
     * Scroll in the specified direction on a scrollable element.
     */
    fun scroll(direction: ScrollDirection): HandlerResult {
        val root = rootInActiveWindow
            ?: return HandlerResult.Error("No active window available")

        return try {
            val scrollable = findScrollableNode(root)
            if (scrollable != null) {
                val scrollAction = when (direction) {
                    ScrollDirection.UP, ScrollDirection.LEFT ->
                        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                    ScrollDirection.DOWN, ScrollDirection.RIGHT ->
                        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                }
                val success = scrollable.performAction(scrollAction)
                scrollable.recycle()
                root.recycle()
                if (success) {
                    val data = buildJsonObject { put("direction", direction.name.lowercase()) }
                    HandlerResult.Success(data)
                } else {
                    HandlerResult.Error("Scroll ${direction.name.lowercase()} failed")
                }
            } else {
                root.recycle()
                HandlerResult.Error("No scrollable element found on screen")
            }
        } catch (e: Exception) {
            Log.e(TAG, "scroll failed", e)
            HandlerResult.Error("scroll failed: ${e.message}")
        }
    }

    // ── Private helpers ──

    private fun traverseTree(
        node: AccessibilityNodeInfo,
        depth: Int,
        lines: MutableList<String>,
    ) {
        if (depth > MAX_TREE_DEPTH) return

        val line = NodeTextExtractor.formatNode(
            depth = depth,
            className = node.className?.toString(),
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            isClickable = node.isClickable,
            isScrollable = node.isScrollable,
            resourceId = node.viewIdResourceName,
        )

        // Only add nodes that have meaningful content
        if (node.text != null || node.contentDescription != null ||
            node.isClickable || node.isScrollable ||
            node.viewIdResourceName != null
        ) {
            lines.add(line)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseTree(child, depth + 1, lines)
            child.recycle()
        }
    }

    private fun findMatchingNode(
        node: AccessibilityNodeInfo,
        targetText: String?,
        targetContentDescription: String?,
    ): AccessibilityNodeInfo? {
        val matches = ElementMatcher.matches(
            nodeText = node.text?.toString(),
            nodeContentDescription = node.contentDescription?.toString(),
            targetText = targetText,
            targetContentDescription = targetContentDescription,
        )

        if (matches && node.isClickable) {
            return AccessibilityNodeInfo.obtain(node)
        }

        // If matches but not clickable, check if a clickable parent exists
        if (matches) {
            val clickableParent = findClickableParent(node)
            if (clickableParent != null) return clickableParent
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findMatchingNode(child, targetText, targetContentDescription)
            child.recycle()
            if (result != null) return result
        }

        return null
    }

    private fun findClickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current = node.parent ?: return null
        var depth = 0
        while (depth < 5) { // Don't traverse too far up
            if (current.isClickable) {
                return AccessibilityNodeInfo.obtain(current)
            }
            val parent = current.parent
            current.recycle()
            current = parent ?: return null
            depth++
        }
        current.recycle()
        return null
    }

    private fun findFocusedInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null) return focused

        // Fallback: find first editable field
        return findFirstEditable(root)
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFirstEditable(child)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) {
            return AccessibilityNodeInfo.obtain(root)
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findScrollableNode(child)
            child.recycle()
            if (result != null) return result
        }
        return null
    }
}
