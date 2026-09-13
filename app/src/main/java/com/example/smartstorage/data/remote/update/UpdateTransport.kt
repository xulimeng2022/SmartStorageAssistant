package com.example.smartstorage.data.remote.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** 可替换的更新版本源。 */
interface UpdateTransport {
    suspend fun fetchLatestStable(): Result<UpdateRelease>
}

/** GitHub Releases 稳定渠道实现。 */
@Singleton
class GithubUpdateTransport @Inject constructor(
    private val client: OkHttpClient,
) : UpdateTransport {

    override suspend fun fetchLatestStable(): Result<UpdateRelease> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "SmartStorageAssistant")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val json = JSONObject(response.body?.string().orEmpty())
                check(!json.optBoolean("draft", false)) { "draft release" }
                check(!json.optBoolean("prerelease", false)) { "prerelease" }
                val tag = json.optString("tag_name").trim()
                check(UpdateVersionComparator.parse(tag) != null) { "invalid stable version" }
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url").takeIf(String::isNotBlank)
                            if (apkUrl != null) break
                        }
                    }
                }
                UpdateRelease(
                    versionName = tag.removePrefix("v"),
                    notes = json.optString("body").trim(),
                    downloadUrl = apkUrl ?: json.optString("html_url"),
                    releaseUrl = json.optString("html_url"),
                )
            }
        }
    }

    private companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/xulimeng2022/SmartStorageAssistant/releases/latest"
    }
}