package com.okeho.mapping.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.okeho.mapping.ui.auth.LoginScreen
import com.okeho.mapping.ui.auth.SignupScreen

/**
 * The unauthenticated graph. Neither screen navigates on success -- the gate in
 * MainActivity observes the session and replaces this graph wholesale, which
 * also disposes the back stack so a signed-out user can't navigate back in.
 */
@Composable
fun AuthNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
    }
}
