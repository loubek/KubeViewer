package cz.uhk.KubeViewer

import android.app.Activity
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import androidx.compose.material3.*

@Composable
fun AppNavigator(activity: Activity) {
    val navController = rememberNavController()
    var nodes by remember {
        mutableStateOf(emptyList<Map<String, String>>())
    }
    var pods by remember {
        mutableStateOf(emptyList<Map<String, String>>())
    }
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onNext = { navController.navigate("kubeconfig") })
        }
        composable("kubeconfig") {
            KubeConfigScreen(
                activity = activity,
                onNodesLoaded = {
                    nodes = it
                    navController.navigate("nodes")
                },
                onPodsLoaded = {
                    pods = it
                    navController.navigate("pods")
                }
            )
        }
        composable("nodes") {
            NodeListScreen(nodes)
        }
        composable("pods") {
            PodListScreen(pods)
        }
    }
}
