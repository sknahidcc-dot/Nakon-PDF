package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.about.AboutScreen
import com.example.ui.builder.PdfBuilderScreen
import com.example.ui.camera.ContinuousCameraScreen
import com.example.ui.home.HomeScreen
import com.example.ui.navigation.Screen
import com.example.ui.theme.NakonPdfTheme
import com.example.ui.viewer.PdfViewerScreen
import com.example.ui.viewmodel.ScanViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NakonPdfTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val scanViewModel: ScanViewModel = viewModel()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = scanViewModel,
                                onNavigateToCamera = {
                                    navController.navigate(Screen.ContinuousCamera.route)
                                },
                                onNavigateToBuilder = {
                                    navController.navigate(Screen.Builder.route)
                                },
                                onNavigateToViewer = { pdfId ->
                                    navController.navigate(Screen.Viewer.createRoute(pdfId))
                                },
                                onNavigateToAbout = {
                                    navController.navigate(Screen.About.route)
                                }
                            )
                        }

                        composable(Screen.ContinuousCamera.route) {
                            ContinuousCameraScreen(
                                viewModel = scanViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToBuilder = {
                                    navController.navigate(Screen.Builder.route)
                                }
                            )
                        }

                        composable(Screen.Builder.route) {
                            PdfBuilderScreen(
                                viewModel = scanViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToCamera = {
                                    navController.navigate(Screen.ContinuousCamera.route)
                                },
                                onNavigateToViewer = { pdfId ->
                                    navController.navigate(Screen.Viewer.createRoute(pdfId)) {
                                        popUpTo(Screen.Home.route)
                                    }
                                },
                                onNavigateHome = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = Screen.Viewer.route,
                            arguments = listOf(navArgument("pdfId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val pdfId = backStackEntry.arguments?.getLong("pdfId") ?: 0L
                            PdfViewerScreen(
                                pdfId = pdfId,
                                viewModel = scanViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.About.route) {
                            AboutScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
