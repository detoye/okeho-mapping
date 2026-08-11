package com.okeho.mapping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.okeho.mapping.ui.auth.AuthViewModel
import com.okeho.mapping.ui.auth.SplashScreen
import com.okeho.mapping.ui.navigation.AuthNavHost
import com.okeho.mapping.ui.navigation.OkehoNavHost
import com.okeho.mapping.ui.theme.OkehoTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.gotrue.SessionStatus

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OkehoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthGate()
                }
            }
        }
    }
}

/**
 * Picks the app graph based on the auth session. The session is restored
 * asynchronously, so [SessionStatus.LoadingFromStorage] renders a splash rather
 * than guessing. Swapping the whole graph on sign-out also disposes the old back
 * stack, which is why a signed-out user can't navigate back into the app.
 */
@Composable
private fun AuthGate() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val status by authViewModel.sessionStatus.collectAsState()

    when (status) {
        is SessionStatus.LoadingFromStorage -> SplashScreen()
        is SessionStatus.NetworkError -> SplashScreen(
            message = "Could not check your session. Check your connection.",
            onRetry = { authViewModel.reloadSession() }
        )
        is SessionStatus.Authenticated -> OkehoNavHost()
        else -> AuthNavHost()
    }
}
