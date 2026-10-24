package com.example.ar_project_1.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.resource.ResourceType
import com.example.ar_project_1.ui.assistant.AssistantScreen
import com.example.ar_project_1.ui.auth.AuthScreen
import com.example.ar_project_1.ui.documents.DocumentsScreen
import com.example.ar_project_1.ui.home.HomeScreen
import com.example.ar_project_1.ui.images.ImageDetailScreen
import com.example.ar_project_1.ui.images.ImagesScreen
import com.example.ar_project_1.ui.profile.ProfileEditScreen
import com.example.ar_project_1.ui.profile.ProfileScreen
import com.example.ar_project_1.ui.resource.ResourceFormScreen
import com.example.ar_project_1.ui.resource.ResourceListScreen
import com.example.ar_project_1.ui.settings.SettingsScreen
import com.example.ar_project_1.ui.setup.ServerSetupScreen

object Routes {
    const val SETUP = "setup"
    const val AUTH = "auth"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile/edit"
    const val DOCUMENTS = "documents"
    const val IMAGES = "images"
    const val IMAGE_DETAIL = "images/{imageId}"
    const val ASSISTANT = "assistant"
    const val SETTINGS = "settings"
    const val RESOURCE = "resource/{type}"
    const val RESOURCE_NEW = "resource/{type}/new"
    const val RESOURCE_EDIT = "resource/{type}/edit/{id}"

    fun resource(type: ResourceType) = "resource/${type.route}"
    fun resourceNew(type: ResourceType) = "resource/${type.route}/new"
    fun resourceEdit(type: ResourceType, id: String) = "resource/${type.route}/edit/$id"
    fun imageDetail(id: String) = "images/$id"
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val BOTTOM_DESTINATIONS = listOf(
    BottomDestination(Routes.HOME, "Start", Icons.Default.Home),
    BottomDestination(Routes.DOCUMENTS, "Dokumenty", Icons.Default.Description),
    BottomDestination(Routes.IMAGES, "Zdjęcia", Icons.Default.Image),
    BottomDestination(Routes.ASSISTANT, "Asystent", Icons.Default.SmartToy),
    BottomDestination(Routes.SETTINGS, "Ustawienia", Icons.Default.Settings),
)

private val MAIN_ROUTES = BOTTOM_DESTINATIONS.map { it.route }.toSet()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(container: AppContainer) {
    val navController = rememberNavController()
    val session by container.sessionManager.state.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    val startDestination = remember {
        when {
            session.serverUrl.isNullOrBlank() -> Routes.SETUP
            !session.isLoggedIn -> Routes.AUTH
            else -> Routes.HOME
        }
    }

    fun clearAndNavigate(target: String) {
        navController.navigate(target) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(session.serverUrl, session.isLoggedIn) {
        val current = navController.currentBackStackEntry?.destination?.route
        if (session.serverUrl.isNullOrBlank()) {
            if (current != Routes.SETUP) clearAndNavigate(Routes.SETUP)
        } else if (!session.isLoggedIn) {
            if (current != Routes.AUTH && current != Routes.SETUP) clearAndNavigate(Routes.AUTH)
        } else if (current == Routes.AUTH) {
            clearAndNavigate(Routes.HOME)
        }
    }

    val showChrome = route != null && route != Routes.SETUP && route != Routes.AUTH
    val isMainRoute = route in MAIN_ROUTES

    Scaffold(
        topBar = {
            if (showChrome) {
                TopAppBar(
                    title = { Text(titleFor(route, backStackEntry)) },
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
                )
            }
        },
        bottomBar = {
            if (isMainRoute) {
                NavigationBar {
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
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.SETUP) {
                ServerSetupScreen(
                    container = container,
                    onContinue = { clearAndNavigate(Routes.AUTH) },
                )
            }

            composable(Routes.AUTH) {
                AuthScreen(
                    container = container,
                    onChangeServer = { clearAndNavigate(Routes.SETUP) },
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    onOpenResource = { navController.navigate(Routes.resource(it)) },
                    onProfile = { navController.navigate(Routes.PROFILE) },
                    onDocuments = { navController.navigate(Routes.DOCUMENTS) },
                    onImages = { navController.navigate(Routes.IMAGES) },
                    onAssistant = { navController.navigate(Routes.ASSISTANT) },
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    container = container,
                    onEdit = { navController.navigate(Routes.PROFILE_EDIT) },
                )
            }

            composable(Routes.PROFILE_EDIT) {
                ProfileEditScreen(
                    container = container,
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(Routes.DOCUMENTS) {
                DocumentsScreen(container)
            }

            composable(Routes.IMAGES) {
                ImagesScreen(
                    container = container,
                    onOpenImage = { navController.navigate(Routes.imageDetail(it)) },
                )
            }

            composable(
                route = Routes.IMAGE_DETAIL,
                arguments = listOf(navArgument("imageId") { type = NavType.StringType }),
            ) { entry ->
                ImageDetailScreen(
                    container = container,
                    imageId = entry.arguments?.getString("imageId").orEmpty(),
                )
            }

            composable(Routes.ASSISTANT) {
                AssistantScreen(container)
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container = container,
                    onLoggedOut = { clearAndNavigate(Routes.AUTH) },
                    onServerChanged = { clearAndNavigate(Routes.AUTH) },
                )
            }

            composable(
                route = Routes.RESOURCE,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) { entry ->
                val type = ResourceType.fromRoute(entry.arguments?.getString("type"))
                ResourceListScreen(
                    container = container,
                    type = type,
                    onAdd = { navController.navigate(Routes.resourceNew(type)) },
                    onEdit = { id -> navController.navigate(Routes.resourceEdit(type, id)) },
                )
            }

            composable(
                route = Routes.RESOURCE_NEW,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) { entry ->
                val type = ResourceType.fromRoute(entry.arguments?.getString("type"))
                ResourceFormScreen(
                    container = container,
                    type = type,
                    id = null,
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.RESOURCE_EDIT,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType },
                ),
            ) { entry ->
                val type = ResourceType.fromRoute(entry.arguments?.getString("type"))
                ResourceFormScreen(
                    container = container,
                    type = type,
                    id = entry.arguments?.getString("id"),
                    onSaved = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun titleFor(route: String?, entry: NavBackStackEntry?): String = when (route) {
    Routes.HOME -> "Karta Pacjenta"
    Routes.PROFILE -> "Profil"
    Routes.PROFILE_EDIT -> "Edytuj profil"
    Routes.DOCUMENTS -> "Dokumenty"
    Routes.IMAGES -> "Zdjęcia"
    Routes.IMAGE_DETAIL -> "Zdjęcie"
    Routes.ASSISTANT -> "Asystent AI"
    Routes.SETTINGS -> "Ustawienia"
    Routes.RESOURCE -> ResourceType.fromRoute(entry?.arguments?.getString("type")).title
    Routes.RESOURCE_NEW -> "Nowy wpis"
    Routes.RESOURCE_EDIT -> "Edytuj wpis"
    else -> "Karta Pacjenta"
}
