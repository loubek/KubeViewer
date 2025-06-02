
package cz.uhk.KubeViewer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.uhk.KubeViewer.ui.theme.FimCalcTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FimCalcTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Kubeconfig Viewer") })
                    }
                ) { padding ->
                    KubeConfigScreen(modifier = Modifier.padding(padding), activity = this)
                }
            }
        }
    }
}

@Composable
fun KubeConfigScreen(modifier: Modifier = Modifier, activity: Activity) {
    var fileName by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf<String?>(null) }
    var apiServer by remember { mutableStateOf<String?>(null) }
    var authToken by remember { mutableStateOf<String?>(null) }
    var nodeList by remember { mutableStateOf<List<String>?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) {
            Log.e("KUBE", "Soubor nebyl vybrán")
            return@rememberLauncherForActivityResult
        }

        try {
            activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            fileName = uri.lastPathSegment
            val content = readTextFromUri(activity, uri)
            fileContent = content
            Log.d("KUBE", "Soubor načten:\n${content.take(300)}")

            val kubeConfigData = parseKubeConfig(content)
            apiServer = kubeConfigData?.server
            authToken = kubeConfigData?.token

            Log.d("KUBE", "API server: $apiServer")
            Log.d("KUBE", "Token: ${authToken?.take(20)}")

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
                        nodeList = result
                        Log.d("KUBE", "Výsledek: $result")
                    }
                } else {
                    Log.e("KUBE", "API nebo token chybí")
                }
            },
            enabled = !apiServer.isNullOrEmpty() && !authToken.isNullOrEmpty()
        ) {
            Text("Zobrazit nody")
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

        nodeList?.let { list ->
            Text("Seznam nodů:")
            list.forEach { name ->
                Text("- $name")
            }
        }
    }
}

fun readTextFromUri(activity: Activity, uri: Uri): String {
    val inputStream = activity.contentResolver.openInputStream(uri)
    val reader = BufferedReader(InputStreamReader(inputStream))
    return reader.readText()
}
