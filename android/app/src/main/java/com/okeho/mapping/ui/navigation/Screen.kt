package com.okeho.mapping.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Capture : Screen("capture")
    data object CaptureDetails : Screen("capture_details/{featureType}") {
        fun createRoute(featureType: String) = "capture_details/$featureType"
    }
    data object StreetMapping : Screen("street_mapping")
    data object StreetDetails : Screen("street_details")
    data object Records : Screen("records")
    data object Settings : Screen("settings")
    data object Login : Screen("login")
    data object Signup : Screen("signup")
}
