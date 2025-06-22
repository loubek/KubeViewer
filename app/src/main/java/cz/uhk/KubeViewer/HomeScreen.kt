package cz.uhk.KubeViewer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onNext: () -> Unit, onImagePick: () -> Unit, onCameraPick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("KubeConfig Viewer", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNext) {
            Text("Start")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onImagePick) {
            Text("Nasenovat Kubeconfig")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onCameraPick) {
            Text("Vyfotit Kubeconfig")
        }
    }
}
