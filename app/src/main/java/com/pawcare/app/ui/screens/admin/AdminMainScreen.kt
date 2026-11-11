package com.pawcare.app.ui.screens.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LocalHospital
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
import com.pawcare.app.ui.navigation.AdminTab
import com.pawcare.app.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    viewModel: AdminViewModel,
    onNavigateToInputRecord: (String) -> Unit,
    onNavigateToMedicalDetail: (String) -> Unit,
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf(AdminTab.QUEUE) }
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
            title = "Keluar dari Portal Admin",
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
                        text = "PawCare • Clinic Admin",
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
                    selected = currentTab == AdminTab.QUEUE,
                    onClick = { currentTab = AdminTab.QUEUE },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) },
                    label = { Text(AdminTab.QUEUE.title) }
                )
                NavigationBarItem(
                    selected = currentTab == AdminTab.ACTIVE,
                    onClick = { currentTab = AdminTab.ACTIVE },
                    icon = { Icon(Icons.Default.LocalHospital, contentDescription = null) },
                    label = { Text(AdminTab.ACTIVE.title) }
                )
                NavigationBarItem(
                    selected = currentTab == AdminTab.ARCHIVE,
                    onClick = { currentTab = AdminTab.ARCHIVE },
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    label = { Text(AdminTab.ARCHIVE.title) }
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
                AdminTab.QUEUE -> AdminQueueScreen(viewModel = viewModel)
                AdminTab.ACTIVE -> AdminActivePatientsScreen(
                    viewModel = viewModel,
                    onInputRecordClick = onNavigateToInputRecord
                )
                AdminTab.ARCHIVE -> AdminArchiveScreen(
                    viewModel = viewModel,
                    onDetailClick = onNavigateToMedicalDetail
                )
            }
        }
    }
}
