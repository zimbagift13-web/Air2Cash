package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.admin.AdminPortalScreen
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.OtpVerificationScreen
import com.example.ui.screens.auth.RegistrationScreen
import com.example.ui.screens.auth.SplashScreen
import com.example.ui.screens.auth.WelcomeScreen
import com.example.ui.screens.convert.ConvertAirtimeScreen
import com.example.ui.screens.convert.TransferInstructionScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.notifications.NotificationScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.SecuritySettingsScreen
import com.example.ui.screens.profile.SettingsScreen
import com.example.ui.screens.referrals.ReferralScreen
import com.example.ui.screens.support.SupportScreen
import com.example.ui.screens.transactions.TransactionsScreen
import com.example.ui.screens.wallet.WalletScreen
import com.example.ui.screens.withdraw.WithdrawScreen
import com.example.ui.screens.withdraw.WithdrawalConfirmationScreen
import com.example.ui.viewmodel.Air2CashViewModel

sealed class Screen(
    val route: String,
    val title: String = "",
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object OtpVerify : Screen("otp_verify/{phone}/{otp}") {
        fun createRoute(phone: String, otp: String) = "otp_verify/$phone/$otp"
    }
    object ForgotPassword : Screen("forgot_password")

    // Bottom Navigation Bar tabs
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Convert : Screen("convert", "Convert", Icons.Filled.CurrencyExchange, Icons.Outlined.CurrencyExchange)
    object Wallet : Screen("wallet", "Wallet", Icons.Filled.Wallet, Icons.Outlined.Wallet)
    object Transactions : Screen("transactions", "History", Icons.Filled.History, Icons.Outlined.History)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)

    // Sub-screens
    object TransferInstructions : Screen("transfer_instructions/{reference}") {
        fun createRoute(reference: String) = "transfer_instructions/$reference"
    }
    object Withdraw : Screen("withdraw")
    object WithdrawConfirmation : Screen("withdraw_confirm/{reference}") {
        fun createRoute(reference: String) = "withdraw_confirm/$reference"
    }
    object Referrals : Screen("referrals")
    object Notifications : Screen("notifications")
    object Support : Screen("support?ref={ref}") {
        fun createRoute(ref: String? = null) = if (ref != null) "support?ref=$ref" else "support"
    }
    object SecuritySettings : Screen("security_settings")
    object Settings : Screen("settings")
    object AdminPortal : Screen("admin_portal")
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Convert,
    Screen.Wallet,
    Screen.Transactions,
    Screen.Profile
)

@Composable
fun Air2CashNavGraph(
    viewModel: Air2CashViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentUser by viewModel.currentUser.collectAsState()

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = (if (selected) screen.selectedIcon else screen.unselectedIcon) ?: Icons.Default.Home,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Splash Screen
            composable(Screen.Splash.route) {
                SplashScreen(
                    onFinished = {
                        if (currentUser != null) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Welcome.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // 2. Welcome Screen
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onLoginClick = { navController.navigate(Screen.Login.route) },
                    onRegisterClick = { navController.navigate(Screen.Register.route) },
                    onQuickDemoClick = {
                        viewModel.login("+265991234567", "user123") { success, _ ->
                            if (success) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }

            // 3. Login Screen
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 4. Registration Screen
            composable(Screen.Register.route) {
                RegistrationScreen(
                    viewModel = viewModel,
                    onRegistered = { phone, otp ->
                        navController.navigate(Screen.OtpVerify.createRoute(phone, otp))
                    },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 5. OTP Verification Screen
            composable(
                route = Screen.OtpVerify.route,
                arguments = listOf(
                    navArgument("phone") { type = NavType.StringType },
                    navArgument("otp") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val phone = backStackEntry.arguments?.getString("phone") ?: ""
                val otp = backStackEntry.arguments?.getString("otp") ?: ""
                OtpVerificationScreen(
                    phone = phone,
                    initialOtp = otp,
                    viewModel = viewModel,
                    onVerified = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 6. Forgot Password Screen
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    viewModel = viewModel,
                    onSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 7. Home Dashboard
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToConvert = { navController.navigate(Screen.Convert.route) },
                    onNavigateToWithdraw = { navController.navigate(Screen.Withdraw.route) },
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToReferrals = { navController.navigate(Screen.Referrals.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToSupport = { navController.navigate(Screen.Support.createRoute()) },
                    onNavigateToAdmin = { navController.navigate(Screen.AdminPortal.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToDetail = { _, _ -> navController.navigate(Screen.Transactions.route) }
                )
            }

            // 8. Convert Airtime
            composable(Screen.Convert.route) {
                ConvertAirtimeScreen(
                    viewModel = viewModel,
                    onContinueToTransfer = { txn ->
                        navController.navigate(Screen.TransferInstructions.createRoute(txn.reference))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 9. Transfer Instructions (USSD & Verify)
            composable(
                route = Screen.TransferInstructions.route,
                arguments = listOf(navArgument("reference") { type = NavType.StringType })
            ) { backStackEntry ->
                val ref = backStackEntry.arguments?.getString("reference") ?: ""
                TransferInstructionScreen(
                    reference = ref,
                    viewModel = viewModel,
                    onVerifiedSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Convert.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 10. Wallet Screen
            composable(Screen.Wallet.route) {
                WalletScreen(
                    viewModel = viewModel,
                    onNavigateToConvert = { navController.navigate(Screen.Convert.route) },
                    onNavigateToWithdraw = { navController.navigate(Screen.Withdraw.route) },
                    onSelectLedgerEntry = { /* Ledger detail */ }
                )
            }

            // 11. Withdraw Screen
            composable(Screen.Withdraw.route) {
                WithdrawScreen(
                    viewModel = viewModel,
                    onWithdrawSuccess = { wdr ->
                        navController.navigate(Screen.WithdrawConfirmation.createRoute(wdr.reference)) {
                            popUpTo(Screen.Withdraw.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 12. Withdraw Confirmation Screen
            composable(
                route = Screen.WithdrawConfirmation.route,
                arguments = listOf(navArgument("reference") { type = NavType.StringType })
            ) { backStackEntry ->
                val ref = backStackEntry.arguments?.getString("reference") ?: ""
                WithdrawalConfirmationScreen(
                    reference = ref,
                    viewModel = viewModel,
                    onDone = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            // 13. Transactions History Screen
            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    viewModel = viewModel,
                    onReportSupportWithRef = { ref ->
                        navController.navigate(Screen.Support.createRoute(ref))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 14. Referrals Screen
            composable(Screen.Referrals.route) {
                ReferralScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 15. Notifications Screen
            composable(Screen.Notifications.route) {
                NotificationScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 16. Support Screen
            composable(
                route = Screen.Support.route,
                arguments = listOf(
                    navArgument("ref") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val initialRef = backStackEntry.arguments?.getString("ref")
                SupportScreen(
                    initialTransactionRef = initialRef,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 17. Profile Screen
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToSecurity = { navController.navigate(Screen.SecuritySettings.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToAdmin = { navController.navigate(Screen.AdminPortal.route) },
                    onLogout = {
                        viewModel.logout()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // 18. Security Settings
            composable(Screen.SecuritySettings.route) {
                SecuritySettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 19. Settings & Legal
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 20. Admin Portal
            composable(Screen.AdminPortal.route) {
                AdminPortalScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
