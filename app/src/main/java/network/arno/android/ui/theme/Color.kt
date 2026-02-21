package network.arno.android.ui.theme

import androidx.compose.ui.graphics.Color

// ── JARVIS Core Palette ──
val JarvisBg = Color(0xFF0A0A0F)          // Deep black background
val JarvisSurface = Color(0xFF0D1117)      // Surface/card background
val JarvisSurfaceVariant = Color(0xFF161B22) // Elevated surface
val JarvisBorder = Color(0xFF21262D)       // Subtle borders

// ── JARVIS Accent Colours (matching web client) ──
val JarvisCyan = Color(0xFF00FFFF)         // Primary accent, user messages
val JarvisGreen = Color(0xFF00FF00)        // Assistant messages
val JarvisYellow = Color(0xFFFFC832)       // System/warning
val JarvisRed = Color(0xFFFF6464)          // Error
val JarvisMagenta = Color(0xFFFF00FF)      // Tool calls
val JarvisBlue = Color(0xFF0088FF)         // Skills/links

// ── Text Colours ──
val JarvisText = Color(0xFFE0E0E0)         // Primary text
val JarvisTextSecondary = Color(0xFF8B949E) // Secondary/muted text
val JarvisTimestamp = Color(0x66FFFFFF)     // 40% white for timestamps

// ── Message Background Tints (subtle, matching web client rgba overlays) ──
val JarvisUserBg = Color(0x0800FFFF)       // Very subtle cyan tint
val JarvisAssistantBg = Color(0x0800FF00)  // Very subtle green tint
val JarvisToolBg = Color(0x08FF00FF)       // Very subtle magenta tint
val JarvisSystemBg = Color(0x08FFC832)     // Very subtle yellow tint
val JarvisErrorBg = Color(0x0DFF6464)      // Subtle red tint

// ── Glow Colours (for emphasis/active states) ──
val JarvisCyanGlow = Color(0x8000FFFF)     // 50% cyan for glow effects
val JarvisGreenGlow = Color(0x8000FF00)    // 50% green for glow effects

// ── Legacy aliases (for compatibility) ──
val ArnoDark = JarvisBg
val ArnoSurface = JarvisSurface
val ArnoSurfaceVariant = JarvisSurfaceVariant
val ArnoBorder = JarvisBorder
val ArnoAccent = JarvisCyan
val ArnoGreen = JarvisGreen
val ArnoRed = JarvisRed
val ArnoYellow = JarvisYellow
val ArnoText = JarvisText
val ArnoTextSecondary = JarvisTextSecondary
val ArnoUserBubble = JarvisSurface
val ArnoAssistantBubble = JarvisSurface
