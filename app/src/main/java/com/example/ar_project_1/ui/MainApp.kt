package com.example.ar_project_1.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.model.AiStatusDto
import com.example.ar_project_1.data.model.SearchHitDto
import com.example.ar_project_1.data.model.TimelineEventDto
import com.example.ar_project_1.data.resource.ResourceType
import com.example.ar_project_1.ui.assistant.AssistantScreen
import com.example.ar_project_1.ui.auth.AuthScreen
import com.example.ar_project_1.ui.charts.ChartsScreen
import com.example.ar_project_1.ui.common.LocalAiStatus
import com.example.ar_project_1.ui.documents.DocumentsScreen
import com.example.ar_project_1.ui.home.HomeScreen
import com.example.ar_project_1.ui.images.ImageDetailScreen
import com.example.ar_project_1.ui.images.ImagesScreen
import com.example.ar_project_1.ui.onboarding.WelcomeScreen
import com.example.ar_project_1.ui.profile.ProfileEditScreen
import com.example.ar_project_1.ui.profile.ProfileScreen
import com.example.ar_project_1.ui.resource.ResourceFormScreen
import com.example.ar_project_1.ui.resource.ResourceListScreen
import com.example.ar_project_1.ui.search.SearchScreen
import com.example.ar_project_1.ui.settings.SettingsScreen
import com.example.ar_project_1.ui.timeline.TimelineScreen

object Routes {
    const val WELCOME = "welcome"
    const val AUTH = "auth"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile/edit"
    const val DOCUMENTS = "documents"
    const val IMAGES = "images"
    const val IMAGE_DETAIL = "images/{imageId}"
    const val ASSISTANT = "assistant"
    const val SETTINGS = "settings"
    const val TIMELINE = "timeline"
    const val SEARCH = "search"
    const val CHARTS = "charts"
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
    BottomDestination(Routes.DOCUMENTS, "Dokumenty", Icons.Default.Description),
    BottomDestination(Routes.ASSISTANT, "Asystent", Icons.Default.SmartToy),
    BottomDestination(Routes.HOME, "Start", Icons.Default.Home),
    BottomDestination(Routes.IMAGES, "Zdjęcia", Icons.Default.Image),
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
            composable(Routes.WELCOME) {
                WelcomeScreen(
                    onContinue = {
                        container.uiPreferences.welcomeSeen = true
                        clearAndNavigate(
                            if (container.sessionManager.current().isLoggedIn) Routes.HOME else Routes.AUTH,
                        )
                    },
                )
            }

            composable(Routes.AUTH) {
                AuthScreen(container = container)
            }

            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    onOpenResource = { navController.navigate(Routes.resource(it)) },
                    onProfile = { navController.navigate(Routes.PROFILE) },
                    onDocuments = { navController.navigate(Routes.DOCUMENTS) },
                    onImages = { navController.navigate(Routes.IMAGES) },
                    onAssistant = { navController.navigate(Routes.ASSISTANT) },
                    onTimeline = { navController.navigate(Routes.TIMELINE) },
                    onCharts = { navController.navigate(Routes.CHARTS) },
                )
            }

            composable(Routes.TIMELINE) {
                TimelineScreen(
                    container = container,
                    onOpenEvent = { event -> navigateForTimelineEvent(navController, event) },
                )
            }

            composable(Routes.CHARTS) {
                ChartsScreen(container)
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    container = container,
                    onOpenHit = { hit -> navigateForSearchHit(navController, hit) },
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
                    textScale = textScale,
                    onTextScaleChange = { value ->
                        val clamped = value.coerceIn(0.9f, 1.4f)
                        container.uiPreferences.textScale = clamped
                        textScale = clamped
                    },
                    onLoggedOut = { clearAndNavigate(Routes.AUTH) },
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
}

private fun navigateForTimelineEvent(navController: androidx.navigation.NavHostController, event: TimelineEventDto) {
    val type = when (event.type) {
        "APPOINTMENT" -> ResourceType.APPOINTMENTS
        "MEASUREMENT" -> ResourceType.MEASUREMENTS
        "SYMPTOM" -> ResourceType.SYMPTOMS
        "MEDICATION" -> ResourceType.MEDICATIONS
        "DOCUMENT" -> null
        "IMAGE" -> null
        else -> null
    }
    when (event.type) {
        "DOCUMENT" -> navController.navigate(Routes.DOCUMENTS)
        "IMAGE" -> navController.navigate(Routes.IMAGES)
        else -> type?.let { navController.navigate(Routes.resource(it)) }
    }
}

private fun navigateForSearchHit(navController: androidx.navigation.NavHostController, hit: SearchHitDto) {
    when (hit.type) {
        "ALLERGY" -> navController.navigate(Routes.resource(ResourceType.ALLERGIES))
        "CONDITION" -> navController.navigate(Routes.resource(ResourceType.CONDITIONS))
        "MEDICATION" -> navController.navigate(Routes.resource(ResourceType.MEDICATIONS))
        "SYMPTOM" -> navController.navigate(Routes.resource(ResourceType.SYMPTOMS))
        "MEASUREMENT" -> navController.navigate(Routes.resource(ResourceType.MEASUREMENTS))
        "DOCTOR" -> navController.navigate(Routes.resource(ResourceType.DOCTORS))
        "APPOINTMENT" -> navController.navigate(Routes.resource(ResourceType.APPOINTMENTS))
        "DOCUMENT" -> navController.navigate(Routes.DOCUMENTS)
        "IMAGE" -> navController.navigate(Routes.IMAGES)
    }
}

@Composable
private fun ConnectingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(
            "Łączenie z serwerem…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
    Routes.TIMELINE -> "Chronologia"
    Routes.SEARCH -> "Szukaj"
    Routes.CHARTS -> "Wykresy pomiarów"
    Routes.RESOURCE -> ResourceType.fromRoute(entry?.arguments?.getString("type")).title
    Routes.RESOURCE_NEW -> "Nowy wpis"
    Routes.RESOURCE_EDIT -> "Edytuj wpis"
    else -> "Karta Pacjenta"
}
