package cz.uhk.KubeViewer

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cz.uhk.KubeViewer.ui.theme.KubeViewerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KubeViewerTheme {
                AppNavigator(activity = this)
            }
        }
    }
}
