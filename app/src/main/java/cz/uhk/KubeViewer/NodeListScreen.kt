package cz.uhk.KubeViewer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NodeListScreen(nodes: List<Map<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Text("Seznam nodů:", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        if (nodes.isNotEmpty()) {
            val headers = nodes.first().keys.toList()

            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                headers.forEach { header ->
                    Text(
                        text = header,
                        modifier = Modifier
                            .width(100.dp)
                            .padding(end = 8.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Divider()

            nodes.forEach { row ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    headers.forEach { key ->
                        Text(
                            text = row[key] ?: "--",
                            modifier = Modifier
                                .width(100.dp)
                                .padding(end = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            Text("Žádná data k zobrazení.")
        }
    }
}
