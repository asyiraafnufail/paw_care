package com.pawcare.app.ui.screens.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawcare.app.data.model.Role
import com.pawcare.app.ui.components.PawCareLogoBadge
import com.pawcare.app.ui.theme.PawOnPrimary
import com.pawcare.app.ui.theme.PawOnPrimaryContainer
import com.pawcare.app.ui.theme.PawPrimary
import com.pawcare.app.viewmodel.AuthViewModel

@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    onNavigateToOwner: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkSession()
    }

    LaunchedEffect(uiState.isCheckingSession, uiState.authenticatedRole) {
        if (!uiState.isCheckingSession) {
            when (uiState.authenticatedRole) {
                Role.ADMIN -> onNavigateToAdmin()
                Role.OWNER -> onNavigateToOwner()
                null -> onNavigateToLogin()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PawPrimary, PawOnPrimaryContainer)
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PawCareLogoBadge(
                size = 104.dp,
                modifier = Modifier.scale(scale)
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "PawCare",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = PawOnPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Klinik Kesehatan Hewan & Vaksinasi",
                    style = MaterialTheme.typography.labelLarge,
                    color = PawOnPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(36.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                color = PawOnPrimary,
                strokeWidth = 3.dp
            )
        }

        Text(
            text = "Sahabat Terpercaya Anabul Anda • v1.0",
            style = MaterialTheme.typography.labelSmall,
            color = PawOnPrimary.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }
}
