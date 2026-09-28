package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object ContinuousCamera : Screen("continuous_camera")
    data object Builder : Screen("builder")
    data object Viewer : Screen("viewer/{pdfId}") {
        fun createRoute(pdfId: Long) = "viewer/$pdfId"
    }
    data object About : Screen("about")
}
