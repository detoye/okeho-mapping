package com.okeho.mapping.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.okeho.mapping.ui.dashboard.DashboardScreen
import com.okeho.mapping.ui.capture.CaptureScreen
import com.okeho.mapping.ui.capture.CaptureDetailsScreen
import com.okeho.mapping.ui.street.StreetMappingScreen
import com.okeho.mapping.ui.street.StreetDetailsScreen
import com.okeho.mapping.ui.records.RecordsScreen
import com.okeho.mapping.ui.settings.SettingsScreen

@Composable
fun OkehoNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onCapturePoint = { navController.navigate(Screen.Capture.route) },
                onMapStreet = { navController.navigate(Screen.StreetMapping.route) },
                onRecords = { navController.navigate(Screen.Records.route) },
                onSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Capture.route) {
            CaptureScreen(
                onFeatureSelected = { featureType ->
                    navController.navigate(Screen.CaptureDetails.createRoute(featureType))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CaptureDetails.route,
            arguments = listOf(navArgument("featureType") { type = NavType.StringType })
        ) { backStackEntry ->
            val featureType = backStackEntry.arguments?.getString("featureType") ?: ""
            CaptureDetailsScreen(
                featureType = featureType,
                onSaved = {
                    navController.popBackStack(Screen.Dashboard.route, false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StreetMapping.route) {
            StreetMappingScreen(
                onEndSegment = { navController.navigate(Screen.StreetDetails.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StreetDetails.route) {
            StreetDetailsScreen(
                onSaved = {
                    navController.popBackStack(Screen.Dashboard.route, false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Records.route) {
            RecordsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
