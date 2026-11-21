package com.example.ar_project_1.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.resource.ResourceType
import com.example.ar_project_1.ui.assistant.AssistantScreen
import com.example.ar_project_1.ui.auth.AuthScreen
import com.example.ar_project_1.ui.charts.ChartsScreen
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

/** All app destinations; kept apart from [MainApp] so the scaffold stays small. */
internal fun NavGraphBuilder.appGraph(
    container: AppContainer,
    navController: NavHostController,
    clearAndNavigate: (String) -> Unit,
    textScale: () -> Float,
    onTextScaleChange: (Float) -> Unit,
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
        AssistantScreen(
            container = container,
            onOpenImage = { navController.navigate(Routes.imageDetail(it)) },
        )
    }

    composable(Routes.SETTINGS) {
        SettingsScreen(
            container = container,
            textScale = textScale(),
            onTextScaleChange = onTextScaleChange,
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
