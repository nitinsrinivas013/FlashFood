package com.foodconnect.app

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.foodconnect.core.security.SessionManager
import com.foodconnect.feature.auth.LoginScreen
import com.foodconnect.feature.auth.RegisterScreen
import com.foodconnect.feature.auth.RoleSelectionScreen
import com.foodconnect.feature.restaurant.RestaurantDashboardScreen
import com.foodconnect.feature.restaurant.CreateDonationScreen
import com.foodconnect.feature.ngo.NgoDashboardScreen
import com.foodconnect.feature.donations.DonationDetailScreen
import com.foodconnect.feature.navigation.PickupNavigationScreen
import com.foodconnect.feature.notifications.NotificationsScreen

object Routes {
    const val SPLASH = "splash"
    const val ROLE_SELECTION = "role_selection"
    const val LOGIN = "login"
    const val REGISTER = "register/{role}"
    const val RESTAURANT_DASHBOARD = "restaurant_dashboard"
    const val NGO_DASHBOARD = "ngo_dashboard"
    const val CREATE_DONATION = "create_donation"
    const val DONATION_DETAIL = "donation_detail/{donationId}"
    const val PICKUP_NAVIGATION = "pickup_navigation/{donationId}"
    const val NOTIFICATIONS = "notifications"

    fun register(role: String) = "register/$role"
    fun donationDetail(id: Long) = "donation_detail/$id"
    fun pickupNavigation(id: Long) = "pickup_navigation/$id"
}

@Composable
fun FoodConnectNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(navController)
        }

        composable(Routes.ROLE_SELECTION) {
            RoleSelectionScreen(
                onSelectRole = { role -> navController.navigate(Routes.register(role)) },
                onLoginClick = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val dest = if (role == "RESTAURANT") Routes.RESTAURANT_DASHBOARD else Routes.NGO_DASHBOARD
                    navController.navigate(dest) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate(Routes.ROLE_SELECTION) }
            )
        }

        composable(
            Routes.REGISTER,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "RESTAURANT"
            RegisterScreen(
                role = role,
                onRegisterSuccess = { r ->
                    val dest = if (r == "RESTAURANT") Routes.RESTAURANT_DASHBOARD else Routes.NGO_DASHBOARD
                    navController.navigate(dest) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.RESTAURANT_DASHBOARD) {
            RestaurantDashboardScreen(
                onCreateDonation = { navController.navigate(Routes.CREATE_DONATION) },
                onDonationClick = { id -> navController.navigate(Routes.donationDetail(id)) },
                onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onLogout = {
                    navController.navigate(Routes.ROLE_SELECTION) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CREATE_DONATION) {
            CreateDonationScreen(
                onDonationCreated = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.NGO_DASHBOARD) {
            NgoDashboardScreen(
                onDonationClick = { id -> navController.navigate(Routes.donationDetail(id)) },
                onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToPickup = { id -> navController.navigate(Routes.pickupNavigation(id)) },
                onLogout = {
                    navController.navigate(Routes.ROLE_SELECTION) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Routes.DONATION_DETAIL,
            arguments = listOf(navArgument("donationId") { type = NavType.LongType })
        ) {
            DonationDetailScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToPickup = { id -> navController.navigate(Routes.pickupNavigation(id)) },
                onAccepted = { navController.popBackStack() }
            )
        }

        composable(
            Routes.PICKUP_NAVIGATION,
            arguments = listOf(navArgument("donationId") { type = NavType.LongType })
        ) {
            PickupNavigationScreen(
                onBackClick = { navController.popBackStack() },
                onCompleted = { navController.popBackStack() }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onDonationClick = { id -> navController.navigate(Routes.donationDetail(id)) }
            )
        }
    }
}
