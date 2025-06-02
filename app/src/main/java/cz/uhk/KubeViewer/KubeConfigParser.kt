
package cz.uhk.KubeViewer

import org.yaml.snakeyaml.*

data class KubeConfigData(
    val currentContext: String,
    val server: String,
    val token: String?
)

fun parseKubeConfig(yamlContent: String): KubeConfigData? {
    val yaml = Yaml()
    val data = yaml.load<Map<String, Any>>(yamlContent)

    val currentContextName = data["current-context"] as? String ?: return null

    val contexts = (data["contexts"] as? List<Map<String, Any>>)?.mapNotNull {
        val name = it["name"] as? String ?: return@mapNotNull null
        val context = it["context"] as? Map<*, *> ?: return@mapNotNull null
        val cluster = context["cluster"] as? String ?: return@mapNotNull null
        val user = context["user"] as? String ?: return@mapNotNull null
        Triple(name, cluster, user)
    } ?: return null

    val context = contexts.find { it.first == currentContextName } ?: return null
    val (ctxName, clusterName, userName) = context

    val clusters = (data["clusters"] as? List<Map<String, Any>>)?.mapNotNull {
        val name = it["name"] as? String ?: return@mapNotNull null
        val cluster = it["cluster"] as? Map<*, *> ?: return@mapNotNull null
        val server = cluster["server"] as? String ?: return@mapNotNull null
        name to server
    }?.toMap() ?: return null

    val users = (data["users"] as? List<Map<String, Any>>)?.mapNotNull {
        val name = it["name"] as? String ?: return@mapNotNull null
        val user = it["user"] as? Map<*, *> ?: return@mapNotNull null
        val token = user["token"] as? String
        name to token
    }?.toMap() ?: return null

    return KubeConfigData(
        currentContext = currentContextName,
        server = clusters[clusterName] ?: return null,
        token = users[userName]
    )
}
