package com.example.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import com.example.player.ui.theme.AppSpring
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.player.ui.transitions.LocalLandscapeExiting
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.example.player.ui.transitions.LocalAnimatedContentScope
import com.example.player.ui.transitions.LocalSharedTransitionScope
import com.example.player.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerApp() {
    val navController   = rememberNavController()
    val coroutineScope  = rememberCoroutineScope()
    // Activity 级共享 ViewModel —— 所有子页面共享同一份数据
    val homeViewModel: HomeViewModel = hiltViewModel()
    val context = LocalContext.current
    // 横屏退出信号：true 时临时为横屏视频开启 SharedBounds（详见 LocalLandscapeExiting 注释）
    var landscapeExiting by remember { mutableStateOf(false) }
    // 桥接 LocalConfiguration.current 到 MutableState，供协程通过 snapshotFlow 观测方向变化
    var deviceOrientation by remember { mutableStateOf(Configuration.ORIENTATION_PORTRAIT) }
    deviceOrientation = LocalConfiguration.current.orientation

    val navigateToPlayer: (Uri) -> Unit = { uri ->
        landscapeExiting = false  // 清除上次横屏退出的残留状态
        // 进入横屏视频：导航前设置方向，60ms fade 接管（SharedBounds 因坐标系冲突禁用）
        // 进入竖屏视频：SharedBounds 缩略图扩展动画
        val isLandscape = homeViewModel.getIsLandscape(uri)
        (context as Activity).requestedOrientation =
            if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else             ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        navController.navigate("player/${Uri.encode(uri.toString())}/$isLandscape")
    }

    // ★ 退出播放器
    //   竖屏：立即重置方向 + popBackStack，SharedBounds 同步收缩回缩略图。
    //   横屏：先等方向真正回到竖屏，再触发 popBackStack，全程事件驱动，无固定延时。
    val navigateBackFromPlayer: () -> Unit = {
        val isLandscape = navController.currentBackStackEntry
            ?.arguments?.getBoolean("isLandscape") ?: false
        if (isLandscape) {
            (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            coroutineScope.launch {
                snapshotFlow { deviceOrientation }
                    .filter { it == Configuration.ORIENTATION_PORTRAIT }
                    .first()

                landscapeExiting = true
                withFrameNanos { }
                navController.popBackStack()

                snapshotFlow { navController.currentBackStackEntry?.destination?.route }
                    .filter { route -> route?.startsWith("player") != true }
                    .first()
                landscapeExiting = false
            }
        } else {
            (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            navController.popBackStack()
        }
    }

    // ── SharedTransitionLayout 包裹整个 NavHost ──────────────────────────────
    // sharedBounds 会在 VideoCard ↔ PlayerScreen 之间做容器变换动画；
    // NavHost 的 fade 缩短到 60ms 仅作背景轻过渡，主动画由 sharedBounds 接管。
    CompositionLocalProvider(LocalLandscapeExiting provides landscapeExiting) {
    SharedTransitionLayout {
        val sharedScope = this

        NavHost(
            navController       = navController,
            startDestination    = "home",
            // iOS 26 页面切换：非播放器路由用 spring scale+fade；
            // 播放器路由（home↔player）仅用极短 fade，主动画由 sharedBounds 接管，
            // 避免 NavHost scale 与 SharedBounds 容器变换产生双重运动冲突。
            enterTransition = {
                val involvesPlayer = targetState.destination.route?.startsWith("player") == true ||
                                     initialState.destination.route?.startsWith("player") == true
                if (involvesPlayer) fadeIn(tween(60))
                else scaleIn(AppSpring.spatial(), 0.97f) + fadeIn(AppSpring.spatial())
            },
            exitTransition = {
                val involvesPlayer = targetState.destination.route?.startsWith("player") == true ||
                                     initialState.destination.route?.startsWith("player") == true
                if (involvesPlayer) fadeOut(tween(60))
                else scaleOut(AppSpring.spatial(), 1.03f) + fadeOut(AppSpring.spatial())
            },
            popEnterTransition = {
                val involvesPlayer = targetState.destination.route?.startsWith("player") == true ||
                                     initialState.destination.route?.startsWith("player") == true
                if (involvesPlayer) fadeIn(tween(60))
                else scaleIn(AppSpring.spatial(), 0.97f) + fadeIn(AppSpring.spatial())
            },
            popExitTransition = {
                val involvesPlayer = targetState.destination.route?.startsWith("player") == true ||
                                     initialState.destination.route?.startsWith("player") == true
                if (involvesPlayer) fadeOut(tween(60))
                else scaleOut(AppSpring.spatial(), 1.03f) + fadeOut(AppSpring.spatial())
            }
        ) {

            composable("home") {
                // 提供 SharedTransition scope 给 VideoCard 使用
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides sharedScope,
                    LocalAnimatedContentScope  provides this
                ) {
                    HomeScreen(
                        navController    = navController,
                        viewModel        = homeViewModel,
                        onVideoSelected  = navigateToPlayer
                    )
                }
            }

            composable("search") {
                // SearchScreen 的 VideoCard 也支持 sharedBounds（从搜索结果进入播放器同样丝滑）
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides sharedScope,
                    LocalAnimatedContentScope  provides this
                ) {
                    SearchScreen(
                        viewModel       = homeViewModel,
                        onVideoSelected = navigateToPlayer,
                        onBack          = { navController.popBackStack() }
                    )
                }
            }

            composable("list") {
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides sharedScope,
                    LocalAnimatedContentScope  provides this
                ) {
                    PlaylistScreen(
                        viewModel       = homeViewModel,
                        onVideoSelected = navigateToPlayer,
                        onBack          = { navController.popBackStack() }
                    )
                }
            }

            composable("profile") {
                ProfileScreen(
                    viewModel = homeViewModel,
                    onBack    = { navController.popBackStack() }
                )
            }

            composable(
                route     = "player/{uri}/{isLandscape}",
                arguments = listOf(
                    navArgument("uri")         { type = NavType.StringType },
                    navArgument("isLandscape") { type = NavType.BoolType  }
                )
            ) { backStackEntry ->
                val uriString   = backStackEntry.arguments?.getString("uri")          ?: ""
                val isLandscape = backStackEntry.arguments?.getBoolean("isLandscape") ?: false
                // 提供 SharedTransition scope 给 PlayerScreen 使用
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides sharedScope,
                    LocalAnimatedContentScope  provides this
                ) {
                    PlayerScreen(
                        videoUri          = Uri.parse(uriString),
                        isLandscapeSource = isLandscape,
                        onBack            = navigateBackFromPlayer
                    )
                }
            }
        }
    }
    } // CompositionLocalProvider(LocalLandscapeExiting)
}
