package network.arno.android.command

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Handles open_app and list_apps commands — launches apps by name or
 * package and lists installed launchable apps.
 *
 * Uses [AppLauncherMatcher] for fuzzy name resolution (testable pure logic)
 * and Android [PackageManager] for app discovery and launching.
 *
 * ## open_app payload:
 * ```json
 * { "name": "Instagram" }   // fuzzy match by display name
 * { "package": "com.Slack" } // direct launch by package name
 * ```
 *
 * ## list_apps payload:
 * ```json
 * {}  // no params needed
 * ```
 */
class AppLauncherHandler(private val context: Context) {

    companion object {
        private const val TAG = "AppLauncherHandler"
    }

    fun handleOpenApp(payload: JsonObject): HandlerResult {
        val packageName = payload["package"]?.jsonPrimitive?.content
        val appName = payload["name"]?.jsonPrimitive?.content

        // Direct launch by package name
        if (packageName != null) {
            return launchByPackage(packageName)
        }

        // Fuzzy match by app name
        if (appName != null) {
            return launchByName(appName)
        }

        return HandlerResult.Error("Missing 'name' or 'package' in payload")
    }

    fun handleListApps(@Suppress("UNUSED_PARAMETER") payload: JsonObject): HandlerResult {
        return try {
            val apps = getLaunchableApps()
            val appsArray = buildJsonArray {
                for (app in apps) {
                    add(buildJsonObject {
                        put("name", app.name)
                        put("package", app.packageName)
                    })
                }
            }
            val data = buildJsonObject {
                put("apps", appsArray)
                put("count", apps.size)
            }
            HandlerResult.Success(data)
        } catch (e: Exception) {
            Log.e(TAG, "list_apps failed", e)
            HandlerResult.Error("list_apps failed: ${e.message}")
        }
    }

    private fun launchByPackage(packageName: String): HandlerResult {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return HandlerResult.Error("App not found: $packageName")

        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.i(TAG, "Launched app by package: $packageName")
            val data = buildJsonObject {
                put("package", packageName)
                put("status", "launched")
            }
            HandlerResult.Success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
            HandlerResult.Error("Failed to launch $packageName: ${e.message}")
        }
    }

    private fun launchByName(name: String): HandlerResult {
        val apps = getLaunchableApps()
        return when (val result = AppLauncherMatcher.findMatch(name, apps)) {
            is MatchResult.Found -> {
                launchByPackage(result.app.packageName).let { launchResult ->
                    if (launchResult is HandlerResult.Success) {
                        // Enrich with matched name
                        val data = buildJsonObject {
                            put("name", result.app.name)
                            put("package", result.app.packageName)
                            put("status", "launched")
                        }
                        HandlerResult.Success(data)
                    } else {
                        launchResult
                    }
                }
            }
            is MatchResult.NotFound -> {
                val msg = buildString {
                    append("No app found matching: $name")
                    if (result.suggestions.isNotEmpty()) {
                        append(". Did you mean: ")
                        append(result.suggestions.joinToString(", "))
                        append("?")
                    }
                }
                HandlerResult.Error(msg)
            }
        }
    }

    private fun getLaunchableApps(): List<AppInfo> {
        val pm = context.packageManager

        // Primary: standard launcher apps via ACTION_MAIN + CATEGORY_LAUNCHER
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launcherApps = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { resolveInfo ->
                val appLabel = resolveInfo.loadLabel(pm).toString()
                val pkg = resolveInfo.activityInfo.packageName
                AppInfo(appLabel, pkg)
            }

        val knownPackages = launcherApps.map { it.packageName }.toSet()

        // Fallback: find installed apps that have a launch intent but didn't
        // appear in the launcher query (e.g. Facebook/Meta apps)
        val fallbackApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                appInfo.packageName !in knownPackages &&
                    pm.getLaunchIntentForPackage(appInfo.packageName) != null
            }
            .map { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString()
                AppInfo(label, appInfo.packageName)
            }

        return (launcherApps + fallbackApps)
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }
    }
}
