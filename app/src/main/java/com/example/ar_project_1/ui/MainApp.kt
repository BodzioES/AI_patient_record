package com.example.ar_project_1.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.model.AiStatusDto
import com.example.ar_project_1.ui.common.LocalAiStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(container: AppContainer) {
    val navController = rememberNavController()
    val session by container.sessionManager.state.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    val startDestination = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val current = container.sessionManager.current().serverUrl
        val detected = container.serverAutoConfig.resolveFor(current)
        if (detected != null) {
            container.sessionManager.setServerUrl(detected)
        }
        startDestination.value = when {
            !container.uiPreferences.welcomeSeen -> Routes.WELCOME
            !container.sessionManager.current().isLoggedIn -> Routes.AUTH
            else -> Routes.HOME
        }
    }

    fun clearAndNavigate(target: String) {
        navController.navigate(target) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(session.isLoggedIn) {
        val current = navController.currentBackStackEntry?.destination?.route
        if (!session.isLoggedIn) {
            if (current != null && current != Routes.AUTH && current != Routes.WELCOME) {
                clearAndNavigate(Routes.AUTH)
            }
        } else if (current == Routes.AUTH || current == Routes.WELCOME) {
            clearAndNavigate(Routes.HOME)
        }
    }

    val showChrome = route != null && route != Routes.WELCOME && route != Routes.AUTH
    val isMainRoute = route in MAIN_ROUTES

    val start = startDestination.value
    if (start == null) {
        ConnectingScreen()
        return
    }

    var textScale by remember { mutableStateOf(container.uiPreferences.textScale) }
    val baseDensity = LocalDensity.current

    var aiStatus by remember { mutableStateOf<AiStatusDto?>(null) }
    LaunchedEffect(session.isLoggedIn) {
        aiStatus = if (session.isLoggedIn) {
            runCatching { container.repository.aiStatus() }.getOrNull()
        } else {
            null
        }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(baseDensity.density, textScale),
        LocalAiStatus provides aiStatus,
    ) {
        Scaffold(
        topBar = {
            if (showChrome) {
                TopAppBar(
                    title = { Text(titleFor(route, backStackEntry)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    navigationIcon = {
                        if (!isMainRoute) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Wstecz",
                                )
                            }
                        }
                    },
                    actions = {
                        if (route != Routes.SEARCH && route != Routes.WELCOME && route != Routes.AUTH) {
                            IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                                Icon(Icons.Default.Search, contentDescription = "Szukaj")
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (isMainRoute) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    BOTTOM_DESTINATIONS.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route,
                            onClick = {
                                if (route != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(destination.icon, contentDescription = destination.label)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(innerPadding),
        ) {
            appGraph(
                container = container,
                navController = navController,
                clearAndNavigate = ::clearAndNavigate,
                textScale = { textScale },
                onTextScaleChange = { value ->
                    val clamped = value.coerceIn(0.9f, 1.4f)
                    container.uiPreferences.textScale = clamped
                    textScale = clamped
                },
            )
        }
        }
    }
}
