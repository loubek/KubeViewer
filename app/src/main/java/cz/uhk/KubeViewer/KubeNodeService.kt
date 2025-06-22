package cz.uhk.KubeViewer

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Duration
import java.time.Instant


suspend fun getNodeNames(server: String, token: String): List<Map<String, String>> =
    fetchKubeItems("$server/api/v1/nodes", token) { item ->
        val meta = item.getJSONObject("metadata")
        val status = item.getJSONObject("status")
        val version = status.getJSONObject("nodeInfo").getString("kubeletVersion")
        val age = calculateAge(meta.optString("creationTimestamp", ""))

        val conditions = status.optJSONArray("conditions")
        val readyStatus = if (conditions != null) {
            (0 until conditions.length())
                .map { conditions.getJSONObject(it) }
                .firstOrNull { it.getString("type") == "Ready" }
                ?.getString("status")
        } else null
        val nodeStatus = if (readyStatus == "True") "Ready" else "NotReady"

        val labels = meta.optJSONObject("labels")
        val roles = if (labels != null) {
            labels.keys().asSequence()
                .filter { it.startsWith("node-role.kubernetes.io/") }
                .map { it.removePrefix("node-role.kubernetes.io/") }
                .joinToString(", ").ifEmpty { "<none>" }
        } else "<none>"

        mapOf(
            "NAME" to meta.getString("name"),
            "STATUS" to nodeStatus,
            "ROLES" to roles,
            "AGE" to age,
            "VERSION" to version
        )
    }

suspend fun getPods(server: String, token: String): List<Map<String, String>> =
    fetchKubeItems("$server/api/v1/pods", token) { item ->
        val metadata = item.getJSONObject("metadata")
        val status = item.getJSONObject("status")
        val containers = status.optJSONArray("containerStatuses") ?: return@fetchKubeItems mapOf()

        var readyCount = 0
        var restartCount = 0
        for (i in 0 until containers.length()) {
            val container = containers.getJSONObject(i)
            if (container.optBoolean("ready", false)) readyCount++
            restartCount += container.optInt("restartCount", 0)
        }

        val age = calculateAge(metadata.optString("creationTimestamp", ""))
        mapOf(
            "NAMESPACE" to metadata.getString("namespace"),
            "NAME" to metadata.getString("name"),
            "READY" to "$readyCount/${containers.length()}",
            "STATUS" to status.optString("phase", "Unknown"),
            "RESTARTS" to restartCount.toString(),
            "AGE" to age
        )
    }


private fun calculateAge(creationTimestamp: String): String {
    return try {
        val created = Instant.parse(creationTimestamp)
        val now = Instant.now()
        val duration = Duration.between(created, now)
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        when {
            days > 0 -> "${days}d${hours}h"
            hours > 0 -> "${hours}h${minutes}m"
            else -> "${minutes}m"
        }
    } catch (e: Exception) {
        "--"
    }
}


private fun fetchKubeItems(
    url: String,
    token: String,
    itemMapper: (JSONObject) -> Map<String, String>
): List<Map<String, String>> {
    return try {
        val client = OkHttpClient.Builder()
            .sslSocketFactory(
                InsecureSSLSocketFactory.sslSocketFactory(),
                InsecureSSLSocketFactory.trustManager()
            )
            .hostnameVerifier { _, _ -> true }
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        val response = client.newCall(request).execute()
        val json = response.body?.string()

        if (response.isSuccessful && json != null) {
            val items = JSONObject(json).optJSONArray("items") ?: return emptyList()
            List(items.length()) { i -> itemMapper(items.getJSONObject(i)) }
        } else {
            listOf(mapOf("Error" to "HTTP ${response.code}"))
        }
    } catch (e: Exception) {
        Log.e("KUBE", "Chyba připojení: ${e.message}")
        listOf(mapOf("Error" to "${e.message}"))
    }
}
