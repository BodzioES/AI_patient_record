package com.example.ar_project_1.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.example.ar_project_1.data.model.SearchHitDto
import com.example.ar_project_1.data.model.TimelineEventDto
import com.example.ar_project_1.data.resource.ResourceType

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

internal data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

internal val BOTTOM_DESTINATIONS = listOf(
    BottomDestination(Routes.DOCUMENTS, "Dokumenty", Icons.Default.Description),
    BottomDestination(Routes.ASSISTANT, "Asystent", Icons.Default.SmartToy),
    BottomDestination(Routes.HOME, "Start", Icons.Default.Home),
    BottomDestination(Routes.IMAGES, "Zdjęcia", Icons.Default.Image),
    BottomDestination(Routes.SETTINGS, "Ustawienia", Icons.Default.Settings),
)

internal val MAIN_ROUTES = BOTTOM_DESTINATIONS.map { it.route }.toSet()

internal fun navigateForTimelineEvent(navController: androidx.navigation.NavHostController, event: TimelineEventDto) {
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

internal fun navigateForSearchHit(navController: androidx.navigation.NavHostController, hit: SearchHitDto) {
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
internal fun ConnectingScreen() {
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

internal fun titleFor(route: String?, entry: NavBackStackEntry?): String = when (route) {
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
