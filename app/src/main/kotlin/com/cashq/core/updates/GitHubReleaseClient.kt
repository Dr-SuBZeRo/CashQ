package com.cashq.core.updates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Metadata about a published GitHub Release.
 */
data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val publishedAt: String?
)

/**
 * Result state of an update check.
 */
sealed interface UpdateCheckResult {
    data object Idle : UpdateCheckResult
    data object Checking : UpdateCheckResult
    data class UpToDate(
        val currentVersion: String,
        val latestVersion: String,
        val release: ReleaseInfo
    ) : UpdateCheckResult

    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val release: ReleaseInfo
    ) : UpdateCheckResult

    data class Error(
        val message: String,
        val isRateLimited: Boolean = false,
        val isNotFound: Boolean = false
    ) : UpdateCheckResult
}

/**
 * Client for querying the public GitHub Releases API.
 * Manual-only execution — never called automatically on startup or in background.
 */
object GitHubReleaseClient {

    const val REPO_OWNER = "Dr-SuBZeRo"
    const val REPO_NAME = "CashQ"
    const val REPO_WEB_URL = "https://github.com/$REPO_OWNER/$REPO_NAME"
    private const val API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    /**
     * Checks for updates against the GitHub Releases API.
     *
     * @param currentVersion Installed version string (e.g., "1.0.0").
     */
    suspend fun checkForUpdates(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "CashQ-Android-App")
            }

            val responseCode = connection.responseCode
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val release = parseReleaseJson(responseBody)
                    val comparison = compareVersions(currentVersion, release.versionName)

                    if (comparison < 0) {
                        UpdateCheckResult.UpdateAvailable(
                            currentVersion = currentVersion,
                            latestVersion = release.versionName,
                            release = release
                        )
                    } else {
                        UpdateCheckResult.UpToDate(
                            currentVersion = currentVersion,
                            latestVersion = release.versionName,
                            release = release
                        )
                    }
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    UpdateCheckResult.Error(
                        message = "No published releases found on GitHub repository yet.",
                        isNotFound = true
                    )
                }
                403, 429 -> {
                    UpdateCheckResult.Error(
                        message = "GitHub API rate limit reached. Please try again in a few minutes.",
                        isRateLimited = true
                    )
                }
                else -> {
                    UpdateCheckResult.Error(
                        message = "GitHub returned HTTP $responseCode (${connection.responseMessage})."
                    )
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(
                message = e.localizedMessage ?: "Network error connecting to GitHub. Check your internet connection."
            )
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Parses the JSON payload from the GitHub release endpoint.
     */
    fun parseReleaseJson(jsonString: String): ReleaseInfo {
        val root = JSONObject(jsonString)
        val tagName = root.optString("tag_name", "")
        val versionName = tagName.removePrefix("v").removePrefix("V")
        val name = root.optString("name", tagName)
        val body = root.optString("body", "")
        val htmlUrl = root.optString("html_url", REPO_WEB_URL)
        val publishedAt = if (root.isNull("published_at")) null else root.optString("published_at")

        var apkDownloadUrl: String? = null
        var apkFileName: String? = null

        val assets = root.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    apkDownloadUrl = if (asset.isNull("browser_download_url")) null else asset.optString("browser_download_url")
                    apkFileName = assetName
                    break
                }
            }
        }

        return ReleaseInfo(
            tagName = tagName,
            versionName = versionName,
            name = name,
            body = body,
            htmlUrl = htmlUrl,
            apkDownloadUrl = apkDownloadUrl,
            apkFileName = apkFileName,
            publishedAt = publishedAt
        )
    }

    /**
     * Compares two semantic version strings (e.g., "1.0.0" and "1.10.0").
     *
     * @return Negative integer if v1 < v2, zero if v1 == v2, positive integer if v1 > v2.
     */
    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.trim().removePrefix("v").removePrefix("V").split(".")
            .map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
        val parts2 = v2.trim().removePrefix("v").removePrefix("V").split(".")
            .map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }
}
