
package cz.uhk.KubeViewer

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject


suspend fun getNodeNames(server: String, token: String): List<String> {
    Log.d("KUBE", "getNodeNames START")
    return try {
        Log.d("KUBE", "Připojuji se na: $server")


        val client = OkHttpClient.Builder()
            .sslSocketFactory(
                InsecureSSLSocketFactory.sslSocketFactory(),
                InsecureSSLSocketFactory.trustManager()
            )
            .hostnameVerifier { _, _ -> true }
            .build()

        val request = Request.Builder()
            .url("$server/api/v1/nodes")
            .addHeader("Authorization", "Bearer $token")
            .build()

        val response = client.newCall(request).execute()
        val json = response.body?.string()

        Log.d("KUBE", "HTTP kód: ${response.code}")
        Log.d("KUBE", "JSON: ${json?.take(300)}")

        if (response.isSuccessful && json != null) {
            val items = JSONObject(json).getJSONArray("items")
            List(items.length()) { i ->
                val metadata = items.getJSONObject(i).getJSONObject("metadata")
                metadata.getString("name")
            }
        } else {
            listOf("HTTP \${response.code}")
        }
    } catch (e: Exception) {
        Log.e("KUBE", "Chyba připojení: ${e.message}")
        listOf("Chyba: \${e.message}")
    }
}
