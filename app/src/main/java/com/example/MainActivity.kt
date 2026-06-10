package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MusicViewModel
import com.example.ui.components.CollapsibleSidebar
import com.example.ui.components.ExpandedPlayerView
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistDetailsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.BorderGray
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.MyApplicationTheme

// Standard String Navigation Routes
const val ROUTE_HOME = "home"
const val ROUTE_SEARCH = "search"
const val ROUTE_LIBRARY = "library"
const val ROUTE_PLAYLIST = "playlist/{id}"

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels {
        MusicViewModel.provideFactory(this.application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // EnableEdgeToEdge makes content full-bleed behind system bars
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val currentTrack by viewModel.currentTrack.collectAsState()
                val isPlayerSheetExpanded by viewModel.isPlayerSheetExpanded.collectAsState()

                // Register native BackHandler so a back press collapses the player if open
                if (isPlayerSheetExpanded) {
                    BackHandler {
                        viewModel.isPlayerSheetExpanded.value = false
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MidnightBlack)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CollapsibleSidebar(
                            currentRoute = currentRoute,
                            viewModel = viewModel,
                            onNavigate = { route ->
                                if (route.contains("-1")) {
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                    }
                                } else if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(ROUTE_HOME) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )

                        // Vertical separator hairline between sidebar and content
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(BorderGray)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MidnightBlack,
                        bottomBar = {
                            // Persistent Bottom Music Player Bar displays currently playing track title, artist, and playback controls
                            if (currentTrack != null) {
                                MiniPlayer(
                                    viewModel = viewModel,
                                    onExpand = { viewModel.isPlayerSheetExpanded.value = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .padding(bottom = 6.dp)
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_HOME,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = innerPadding.calculateTopPadding(),
                                    bottom = innerPadding.calculateBottomPadding()
                                )
                        ) {
                            composable(ROUTE_HOME) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToPlaylist = { id ->
                                        navController.navigate("playlist/$id")
                                    }
                                )
                            }

                            composable(ROUTE_SEARCH) {
                                SearchScreen(viewModel = viewModel)
                            }

                            composable(ROUTE_LIBRARY) {
                                LibraryScreen(
                                    viewModel = viewModel,
                                    onNavigateToPlaylist = { id ->
                                        navController.navigate("playlist/$id")
                                    },
                                    onNavigateToFavorites = {
                                        navController.navigate("playlist/-1")
                                    }
                                )
                            }

                            composable(
                                route = ROUTE_PLAYLIST,
                                arguments = listOf(navArgument("id") { type = NavType.IntType })
                            ) { backStackEntry ->
                                val id = backStackEntry.arguments?.getInt("id") ?: -1
                                PlaylistDetailsScreen(
                                    playlistId = id,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                    } // end of Scaffold Box
                    } // end of Sidebar Row

                    // Sliding Overlay Panel for Expanded Playback Control Display
                    AnimatedVisibility(
                        visible = isPlayerSheetExpanded,
                        enter = slideInVertically(
                            initialOffsetY = { height -> height },
                            animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMedium)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { height -> height },
                            animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMedium)
                        ) + fadeOut(),
                        modifier = Modifier.zIndex(100f) // Keep above ALL other scaffold structures
                    ) {
                        ExpandedPlayerView(
                            viewModel = viewModel,
                            onCollapse = { viewModel.isPlayerSheetExpanded.value = false }
                        )
                    }
                }
            }
        }
    }

    // Helper functions
    private fun lineColor(): Color = BorderGray
}
