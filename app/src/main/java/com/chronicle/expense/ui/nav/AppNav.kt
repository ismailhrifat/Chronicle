package com.chronicle.expense.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chronicle.expense.MainViewModel
import com.chronicle.expense.Sheet
import com.chronicle.expense.ui.account.ConfigureScreen
import com.chronicle.expense.ui.account.EditBalanceSheet
import com.chronicle.expense.ui.account.TransferSheet
import com.chronicle.expense.ui.add.AddSheet
import com.chronicle.expense.ui.add.EditTransactionSheet
import com.chronicle.expense.ui.add.EventExpenseSheet
import com.chronicle.expense.ui.add.NewEventSheet
import com.chronicle.expense.ui.debt.AddDebtRecordSheet
import com.chronicle.expense.ui.debt.AddDebtSheet
import com.chronicle.expense.ui.debt.DebtDetailScreen
import com.chronicle.expense.ui.debt.DebtScreen
import com.chronicle.expense.ui.debt.EditDebtRecordSheet
import com.chronicle.expense.ui.events.EventDetailScreen
import com.chronicle.expense.ui.events.EventsScreen
import com.chronicle.expense.ui.home.HomeScreen
import com.chronicle.expense.ui.profile.PlaceholderScreen
import com.chronicle.expense.ui.profile.ProfileScreen
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.transactions.TransactionsScreen

private val tabRoutes = setOf("home", "debt", "events", "profile")

@Composable
fun MainScaffold(vm: MainViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val bottomClearance = BottomBarClearance + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Status bar area is reserved once here, so no screen can scroll content underneath it.
    Box(Modifier.fillMaxSize().background(C.bg).statusBarsPadding()) {
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    bottomPadding = bottomClearance,
                    onSeeAll = { vm.resetPaging(); nav.navigate("transactions") },
                    onConfigure = { nav.navigate("configure/$it") },
                )
            }
            composable("debt") { DebtScreen(vm, bottomClearance, onOpen = { nav.navigate("debtDetail/$it") }) }
            composable(
                "debtDetail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { backStack ->
                DebtDetailScreen(vm, backStack.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable("events") { EventsScreen(vm, bottomClearance, onOpen = { nav.navigate("event/$it") }) }
            composable(
                "event/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { backStack ->
                EventDetailScreen(vm, backStack.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable("profile") { ProfileScreen(vm, bottomClearance) }
            composable("transactions") { TransactionsScreen(vm, onBack = { nav.popBackStack() }) }
            composable(
                "configure/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { backStack ->
                ConfigureScreen(vm, backStack.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
        }

        AnimatedVisibility(
            visible = route == null || route in tabRoutes,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
        ) {
            ChronicleBottomBar(
                current = route,
                onTab = { nav.switchTab(it) },
                onAdd = {
                    vm.sheet = when (route) {
                        "events" -> Sheet.EventExpense()
                        "debt" -> Sheet.AddDebt(vm.debtTab)
                        else -> Sheet.Add()
                    }
                },
            )
        }
    }

    when (val sheet = vm.sheet) {
        is Sheet.Add -> AddSheet(vm, sheet.tab) { vm.sheet = null }
        is Sheet.EditTx -> EditTransactionSheet(vm, sheet.tx) { vm.sheet = null }
        is Sheet.Transfer -> TransferSheet(vm, sheet.accountId) { vm.sheet = null }
        is Sheet.EditBalance -> EditBalanceSheet(vm, sheet.accountId) { vm.sheet = null }
        is Sheet.EventExpense -> EventExpenseSheet(vm, sheet.eventId) { vm.sheet = null }
        Sheet.NewEvent -> NewEventSheet(vm) { vm.sheet = null }
        is Sheet.AddDebt -> AddDebtSheet(vm, sheet.tab) { vm.sheet = null }
        is Sheet.AddDebtRecord -> AddDebtRecordSheet(vm, sheet.debtId) { vm.sheet = null }
        is Sheet.EditDebtRecord -> EditDebtRecordSheet(vm, sheet.debtId, sheet.index) { vm.sheet = null }
        null -> Unit
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo("home") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
