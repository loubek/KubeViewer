package cz.uhk.KubeViewer



import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.withContext



@Composable
fun KubeConfigScreen(
    modifier: Modifier = Modifier,
    activity: Activity,
    onNodesLoaded: (List<Map<String, String>>) -> Unit,
    onPodsLoaded: (List<Map<String, String>>) -> Unit

) {
    var fileName by remember { mutableStateOf<String?>(null) }
    var apiServer by remember { mutableStateOf<String?>(null) }
    var authToken by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) {
            Log.e("KUBE", "Soubor nebyl vybrán")
            return@rememberLauncherForActivityResult
        }

        try {
            activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            fileName = uri.lastPathSegment
            val content = readTextFromUri(activity, uri)
            val kubeConfigData = parseKubeConfig(content)
            apiServer = kubeConfigData?.server
            authToken = kubeConfigData?.token
        } catch (e: Exception) {
            Log.e("KUBE", "Chyba při čtení souboru: ${e.message}", e)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = {
            launcher.launch(arrayOf("*/*"))
        }) {
            Text("Načíst kubeconfig")
        }


        Button(
            onClick = {
                if (!apiServer.isNullOrEmpty() && !authToken.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = getNodeNames(apiServer!!, authToken!!)
                        withContext(Dispatchers.Main) {
                            onNodesLoaded(result)
                        }
                    }
                }
            },
            enabled = !apiServer.isNullOrEmpty() && !authToken.isNullOrEmpty()
        ) {
            Text("Zobrazit nody")
        }
        Button(
            onClick = {
                if (!apiServer.isNullOrEmpty() && !authToken.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = getPods(apiServer!!, authToken!!)
                        withContext(Dispatchers.Main) {
                            onPodsLoaded(result) // 👈 nový callback
                        }
                    }
                }
            },
            enabled = !apiServer.isNullOrEmpty() && !authToken.isNullOrEmpty()
        ) {
            Text("Zobrazit pody")
        }


        fileName?.let {
            Text("Soubor načten: $it")
        }

        apiServer?.let {
            Text("API server: $it")
        }

        authToken?.let {
            Text("Token: ${it.take(20)}...")
        }
    }
}

fun readTextFromUri(activity: Activity, uri: Uri): String {
    val inputStream = activity.contentResolver.openInputStream(uri)
    val reader = BufferedReader(InputStreamReader(inputStream))
    return reader.readText()
}
