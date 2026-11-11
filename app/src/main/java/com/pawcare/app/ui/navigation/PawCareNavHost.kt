package com.pawcare.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pawcare.app.di.AppContainer
import com.pawcare.app.ui.screens.admin.AdminMainScreen
import com.pawcare.app.ui.screens.admin.InputMedicalRecordScreen
import com.pawcare.app.ui.screens.auth.LoginScreen
import com.pawcare.app.ui.screens.auth.RegisterScreen
import com.pawcare.app.ui.screens.auth.SplashScreen
import com.pawcare.app.ui.screens.owner.AddEditPetScreen
import com.pawcare.app.ui.screens.owner.CreateBookingScreen
import com.pawcare.app.ui.screens.owner.MedicalRecordDetailScreen
import com.pawcare.app.ui.screens.owner.OwnerMainScreen
import com.pawcare.app.viewmodel.AdminViewModel
import com.pawcare.app.viewmodel.AuthViewModel
import com.pawcare.app.viewmodel.MedicalRecordDetailViewModel
import com.pawcare.app.viewmodel.OwnerViewModel

@Suppress("UNCHECKED_CAST")
private inline fun <VM : ViewModel> viewModelFactoryOf(crossinline creator: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
    }

@Composable
fun PawCareNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel(
        factory = viewModelFactoryOf {
            AuthViewModel(container.authRepository)
        }
    )

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                viewModel = authViewModel,
                onNavigateToOwner = {
                    navController.navigate(Screen.OwnerGraph.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.AdminGraph.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccessAsOwner = {
                    navController.navigate(Screen.OwnerGraph.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLoginSuccessAsAdmin = {
                    navController.navigate(Screen.AdminGraph.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.OwnerGraph.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        navigation(
            startDestination = Screen.OwnerRoot.route,
            route = Screen.OwnerGraph.route
        ) {
            composable(Screen.OwnerRoot.route) { backStackEntry ->
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.OwnerGraph.route)
                }
                val ownerUser = authState.authenticatedUser
                val ownerViewModel: OwnerViewModel = viewModel(
                    viewModelStoreOwner = graphEntry,
                    factory = viewModelFactoryOf {
                        OwnerViewModel(
                            ownerId = ownerUser?.uid.orEmpty(),
                            ownerEmail = ownerUser?.email.orEmpty(),
                            petRepository = container.petRepository,
                            appointmentRepository = container.appointmentRepository,
                            medicalRecordRepository = container.medicalRecordRepository
                        )
                    }
                )
                OwnerMainScreen(
                    viewModel = ownerViewModel,
                    onNavigateToAddPet = { navController.navigate(Screen.AddEditPet.createRoute(null)) },
                    onNavigateToEditPet = { petId -> navController.navigate(Screen.AddEditPet.createRoute(petId)) },
                    onNavigateToCreateBooking = { navController.navigate(Screen.CreateBooking.route) },
                    onNavigateToMedicalDetail = { appointmentId ->
                        navController.navigate(Screen.MedicalRecordDetail.createRoute(appointmentId))
                    },
                    onLogout = {
                        authViewModel.signOut {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.AddEditPet.routePattern,
                arguments = listOf(navArgument("petId") {
                    type = NavType.StringType
                    defaultValue = "new"
                })
            ) { backStackEntry ->
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.OwnerGraph.route)
                }
                val ownerUser = authState.authenticatedUser
                val ownerViewModel: OwnerViewModel = viewModel(
                    viewModelStoreOwner = graphEntry,
                    factory = viewModelFactoryOf {
                        OwnerViewModel(
                            ownerId = ownerUser?.uid.orEmpty(),
                            ownerEmail = ownerUser?.email.orEmpty(),
                            petRepository = container.petRepository,
                            appointmentRepository = container.appointmentRepository,
                            medicalRecordRepository = container.medicalRecordRepository
                        )
                    }
                )
                val petIdArg = backStackEntry.arguments?.getString("petId")
                val petId = if (petIdArg == "new") null else petIdArg
                AddEditPetScreen(
                    petId = petId,
                    viewModel = ownerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.CreateBooking.route) { backStackEntry ->
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.OwnerGraph.route)
                }
                val ownerUser = authState.authenticatedUser
                val ownerViewModel: OwnerViewModel = viewModel(
                    viewModelStoreOwner = graphEntry,
                    factory = viewModelFactoryOf {
                        OwnerViewModel(
                            ownerId = ownerUser?.uid.orEmpty(),
                            ownerEmail = ownerUser?.email.orEmpty(),
                            petRepository = container.petRepository,
                            appointmentRepository = container.appointmentRepository,
                            medicalRecordRepository = container.medicalRecordRepository
                        )
                    }
                )
                CreateBookingScreen(
                    viewModel = ownerViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddPet = { navController.navigate(Screen.AddEditPet.createRoute(null)) }
                )
            }
        }

        navigation(
            startDestination = Screen.AdminRoot.route,
            route = Screen.AdminGraph.route
        ) {
            composable(Screen.AdminRoot.route) { backStackEntry ->
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.AdminGraph.route)
                }
                val adminViewModel: AdminViewModel = viewModel(
                    viewModelStoreOwner = graphEntry,
                    factory = viewModelFactoryOf {
                        AdminViewModel(
                            appointmentRepository = container.appointmentRepository,
                            medicalRecordRepository = container.medicalRecordRepository
                        )
                    }
                )
                AdminMainScreen(
                    viewModel = adminViewModel,
                    onNavigateToInputRecord = { appointmentId ->
                        navController.navigate(Screen.InputMedicalRecord.createRoute(appointmentId))
                    },
                    onNavigateToMedicalDetail = { appointmentId ->
                        navController.navigate(Screen.MedicalRecordDetail.createRoute(appointmentId))
                    },
                    onLogout = {
                        authViewModel.signOut {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.InputMedicalRecord.routePattern,
                arguments = listOf(navArgument("appointmentId") { type = NavType.StringType })
            ) { backStackEntry ->
                val graphEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.AdminGraph.route)
                }
                val adminViewModel: AdminViewModel = viewModel(
                    viewModelStoreOwner = graphEntry,
                    factory = viewModelFactoryOf {
                        AdminViewModel(
                            appointmentRepository = container.appointmentRepository,
                            medicalRecordRepository = container.medicalRecordRepository
                        )
                    }
                )
                val appointmentId = backStackEntry.arguments?.getString("appointmentId").orEmpty()
                InputMedicalRecordScreen(
                    appointmentId = appointmentId,
                    viewModel = adminViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.MedicalRecordDetail.routePattern,
            arguments = listOf(navArgument("appointmentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId").orEmpty()
            val detailViewModel: MedicalRecordDetailViewModel = viewModel(
                key = "medical_record_$appointmentId",
                factory = viewModelFactoryOf {
                    MedicalRecordDetailViewModel(container.medicalRecordRepository)
                }
            )
            MedicalRecordDetailScreen(
                appointmentId = appointmentId,
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
