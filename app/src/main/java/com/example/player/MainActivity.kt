package com.example.player

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.player.ui.screens.HomeScreen
import com.example.player.ui.screens.PlaylistScreen
import com.example.player.ui.screens.PlayerScreen
import com.example.player.ui.screens.ProfileScreen
import com.example.player.ui.screens.SearchScreen
import com.example.player.ui.theme.PlayerTheme
import com.example.player.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlayerTheme {
                PlayerApp()
            }
        }
    }
}

@Composable
fun PlayerApp() {
    val navController  = rememberNavController()
    // Activity 级共享 ViewModel —— 所有子页面共享同一份数据
    val homeViewModel: HomeViewModel = viewModel()

    val navigateToPlayer: (Uri) -> Unit = { uri ->
        navController.navigate("player/${Uri.encode(uri.toString())}")
    }

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                navController    = navController,
                viewModel        = homeViewModel,
                onVideoSelected  = navigateToPlayer
            )
        }

        composable("search") {
            SearchScreen(
                viewModel       = homeViewModel,
                onVideoSelected = navigateToPlayer,
                onBack          = { navController.popBackStack() }
            )
        }

        composable("list") {
            PlaylistScreen(
                viewModel       = homeViewModel,
                onVideoSelected = navigateToPlayer,
                onBack          = { navController.popBackStack() }
            )
        }

        composable("profile") {
            ProfileScreen(
                viewModel = homeViewModel,
                onBack    = { navController.popBackStack() }
            )
        }

        composable(
            route     = "player/{uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("uri") ?: ""
            PlayerScreen(
                videoUri = Uri.parse(uriString),
                onBack   = { navController.popBackStack() }
            )
        }
    }
}
