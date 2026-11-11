package com.pawcare.app.ui.screens.owner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawcare.app.ui.components.PawCareConfirmDialog
import com.pawcare.app.ui.navigation.OwnerTab
import com.pawcare.app.viewmodel.OwnerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerMainScreen(
    viewModel: OwnerViewModel,
    onNavigateToAddPet: () -> Unit,
    onNavigateToEditPet: (String) -> Unit,
    onNavigateToCreateBooking: () -> Unit,
    onNavigateToMedicalDetail: (String) -> Unit,
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf(OwnerTab.PETS) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(actionMessage, errorMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showLogoutDialog) {
        PawCareConfirmDialog(
            title = "Keluar dari Akun",
            message = "Apakah Anda yakin ingin keluar?",
            confirmText = "Keluar",
            isDestructive = true,
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PawCare • Pet Owner",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == OwnerTab.PETS,
                    onClick = { currentTab = OwnerTab.PETS },
                    icon = { Icon(Icons.Default.Pets, contentDescription = null) },
                    label = { Text(OwnerTab.PETS.title) }
                )
                NavigationBarItem(
                    selected = currentTab == OwnerTab.BOOKINGS,
                    onClick = { currentTab = OwnerTab.BOOKINGS },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text(OwnerTab.BOOKINGS.title) }
                )
                NavigationBarItem(
                    selected = currentTab == OwnerTab.HISTORY,
                    onClick = { currentTab = OwnerTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(OwnerTab.HISTORY.title) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                OwnerTab.PETS -> PetsScreen(
                    viewModel = viewModel,
                    onAddPetClick = onNavigateToAddPet,
                    onEditPetClick = onNavigateToEditPet
                )
                OwnerTab.BOOKINGS -> OwnerBookingsScreen(
                    viewModel = viewModel,
                    onCreateBookingClick = onNavigateToCreateBooking
                )
                OwnerTab.HISTORY -> OwnerHistoryScreen(
                    viewModel = viewModel,
                    onRecordClick = onNavigateToMedicalDetail
                )
            }
        }
    }
}
