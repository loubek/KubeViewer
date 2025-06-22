package cz.uhk.KubeViewer

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch

@Composable
fun ImagePickerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var ocrResult by remember { mutableStateOf<String?>(null) }
    var textToSave by remember { mutableStateOf<String?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        uri?.let {
            recognizeTextFromImageUri(context, it) { recognized ->
                ocrResult = recognized
                textToSave = recognized
            }
        }
    }

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            uri?.let {
                textToSave?.let { text ->
                    scope.launch {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { output ->
                                output.write(text.toByteArray())
                            }
                            Log.d("SAVE_FILE", "Text uložen do: $uri")
                        } catch (e: Exception) {
                            Log.e("SAVE_FILE", "Chyba při ukládání", e)
                        }
                    }
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { pickImageLauncher.launch("image/*") }) {
                Text("Načíst obrázek")
            }
            if (!ocrResult.isNullOrEmpty()) {
                Button(onClick = {
                    createFileLauncher.launch("Kubeconfig_output.txt")
                }) {
                    Text("Uložit")
                }
            }
        }

        imageUri?.let { uri ->
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Zvolený obrázek",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }

        ocrResult?.let {
            Text("Text:", style = MaterialTheme.typography.titleMedium)
            Text(it)
        }
    }
}

fun recognizeTextFromImageUri(context: android.content.Context, uri: Uri, onResult: (String) -> Unit) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                Log.d("OCR", "Text: ${visionText.text}")
                onResult(visionText.text)
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Chyba OCR", e)
                onResult("Chyba při rozpoznání textu: ${e.message}")
            }
    } catch (e: Exception) {
        onResult("Chyba: ${e.message}")
    }
}
