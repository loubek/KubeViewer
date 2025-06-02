package cz.uhk.KubeViewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
                    topBar = { TopAppBar(title = { Text("Kube Viewer") }) }
                ) { padding ->
                    KubeViewerScreen(modifier = Modifier.padding(padding), activity = this)
                }
            }
        }
    }
}

@Composable
fun KubeViewerScreen(modifier: Modifier = Modifier, activity: Activity) {
    val context = activity.applicationContext
    val sharedPrefs = remember { context.getSharedPreferences("kubeviewer", Context.MODE_PRIVATE) }

    var fileName by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf<String?>(sharedPrefs.getString("kubeconfig", null)) }
    var apiServer by remember { mutableStateOf<String?>(null) }
    var authToken by remember { mutableStateOf<String?>(null) }
    var connected by remember { mutableStateOf(false) }
    var outputList by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                fileName = uri.lastPathSegment
                val content = readTextFromUri(activity, uri)
                fileContent = content
                sharedPrefs.edit().putString("kubeconfig", content).apply()

                val kubeConfigData = parseKubeConfig(content)
                apiServer = kubeConfigData?.server
                authToken = kubeConfigData?.token

            } catch (e: Exception) {
                Log.e("KUBE", "Chyba při načítání: ${e.message}", e)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { launcher.launch(arrayOf("*/*")) }) {
                Text("Načíst nový kubeconfig")
            }
            Button(onClick = {
                fileContent?.let {
                    val data = parseKubeConfig(it)
                    apiServer = data?.server
                    authToken = data?.token
                }
            }, enabled = fileContent != null) {
                Text("Použít předchozí")
            }
        }

        authToken?.let { Text("Token: ${it.take(20)}...") }
        apiServer?.let { Text("API server: $it") }

        Button(
            onClick = { connected = true },
            enabled = !apiServer.isNullOrEmpty() && !authToken.isNullOrEmpty()
        ) {
            Text("Připojit")
        }

        if (connected) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val nodes = getNodeNames(apiServer!!, authToken!!)
                        outputList = nodes
                    }
                }) { Text("Node") }

                Button(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val pods = getPods(apiServer!!, authToken!!)
                        outputList = pods
                    }
                }) { Text("Pods") }

                Button(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val namespaces = getNamespaces(apiServer!!, authToken!!)
                        outputList = namespaces
                    }
                }) { Text("Namespace") }

                Button(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val csrs = getCSRs(apiServer!!, authToken!!)
                        outputList = csrs
                    }
                }) { Text("CSR") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (outputList.isNotEmpty()) {
                val headers = outputList.first().keys.toList()
                LazyColumn {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            headers.forEach { header -> Text(header, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge) }
                        }
                    }
                    items(outputList.size) { index ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            headers.forEach { key ->
                                Text(outputList[index][key] ?: "", modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun readTextFromUri(activity: Activity, uri: Uri): String {
    val inputStream = activity.contentResolver.openInputStream(uri)
    val reader = BufferedReader(InputStreamReader(inputStream))
    return reader.readText()
}
