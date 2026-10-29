package com.cashq.core.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseClientTest {

    @Test
    fun compareVersions_sameVersion_returnsZero() {
        assertEquals(0, GitHubReleaseClient.compareVersions("1.0.0", "1.0.0"))
        assertEquals(0, GitHubReleaseClient.compareVersions("v1.0.0", "1.0.0"))
        assertEquals(0, GitHubReleaseClient.compareVersions("1.0", "1.0.0"))
    }

    @Test
    fun compareVersions_minorVersionGreater_handlesCorrectly() {
        // Semantic: 1.10.0 is greater than 1.9.0 (lexicographic would fail this)
        assertTrue(GitHubReleaseClient.compareVersions("1.9.0", "1.10.0") < 0)
        assertTrue(GitHubReleaseClient.compareVersions("1.10.0", "1.9.0") > 0)
    }

    @Test
    fun compareVersions_patchVersionGreater_handlesCorrectly() {
        assertTrue(GitHubReleaseClient.compareVersions("1.0.1", "1.0.2") < 0)
        assertTrue(GitHubReleaseClient.compareVersions("1.0.2", "1.0.1") > 0)
    }

    @Test
    fun compareVersions_majorVersionGreater_handlesCorrectly() {
        assertTrue(GitHubReleaseClient.compareVersions("1.9.9", "2.0.0") < 0)
        assertTrue(GitHubReleaseClient.compareVersions("2.0.0", "1.9.9") > 0)
    }

    @Test
    fun parseReleaseJson_withApkAsset_parsesFieldsCorrectly() {
        val sampleJson = """
            {
              "tag_name": "v1.1.0",
              "name": "CashQ 1.1.0 Release",
              "body": "- Added detailed history export\n- Improved animations",
              "html_url": "https://github.com/Dr-SuBZeRo/CashQ/releases/tag/v1.1.0",
              "published_at": "2026-11-01T10:00:00Z",
              "assets": [
                {
                  "name": "CashQ-v1.1.0.apk",
                  "browser_download_url": "https://github.com/Dr-SuBZeRo/CashQ/releases/download/v1.1.0/CashQ-v1.1.0.apk"
                }
              ]
            }
        """.trimIndent()

        val release = GitHubReleaseClient.parseReleaseJson(sampleJson)

        assertEquals("v1.1.0", release.tagName)
        assertEquals("1.1.0", release.versionName)
        assertEquals("CashQ 1.1.0 Release", release.name)
        assertNotNull(release.apkDownloadUrl)
        assertEquals("CashQ-v1.1.0.apk", release.apkFileName)
    }

    @Test
    fun parseReleaseJson_withoutApkAsset_setsApkUrlToNull() {
        val sampleJson = """
            {
              "tag_name": "v1.0.0",
              "name": "Initial Release",
              "body": "First release of CashQ",
              "html_url": "https://github.com/Dr-SuBZeRo/CashQ/releases/tag/v1.0.0",
              "assets": []
            }
        """.trimIndent()

        val release = GitHubReleaseClient.parseReleaseJson(sampleJson)

        assertEquals("1.0.0", release.versionName)
        assertNull(release.apkDownloadUrl)
    }
}
