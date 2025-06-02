
package cz.uhk.KubeViewer

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

suspend fun getNodesFromApi(server: String, token: String): String {
    return try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("\$server/api/v1/nodes")
            .addHeader("Authorization", "Bearer \$token")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            "Chyba: \${response.code} \${response.message}"
        } else {
            response.body?.string() ?: "Prázdná odpověď"
        }
    } catch (e: IOException) {
        "Chyba připojení: \${e.message}"
    } catch (e: Exception) {
        "Neočekávaná chyba: \${e.message}"
    }
}
