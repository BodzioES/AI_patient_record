package com.example.ar_project_1.ui.settings

import androidx.lifecycle.ViewModel
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.export.ExportData
import com.example.ar_project_1.data.export.ExportSection
import com.example.ar_project_1.data.export.ExportService
import com.example.ar_project_1.data.model.DocumentDto
import com.example.ar_project_1.data.model.ImageDto
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.example.ar_project_1.data.resource.DateInputs
import com.example.ar_project_1.data.resource.ResourceCatalog
import com.example.ar_project_1.data.resource.ResourceType
import com.google.gson.JsonObject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

enum class ExportFormat(val mimeType: String, val fileName: String) {
    CSV("text/csv", "karta-pacjenta.csv"),
    PDF("application/pdf", "karta-pacjenta.pdf"),
}

data class ExportUiState(
    val preparing: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

/**
 * Zbiera wszystkie sekcje karty pacjenta i buduje plik eksportu.
 *
 * Backend ogranicza stronę do 100 elementów, więc dane są dociągane partiami
 * (maksymalnie [MAX_PAGES] stron na sekcję).
 */
class ExportViewModel(
    private val repository: PatientCardRepository,
    private val exportService: ExportService,
) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(ExportUiState())
        private set

    /** Buduje plik w wybranym formacie. Zwraca `null`, jeśli się nie udało. */
    suspend fun build(format: ExportFormat): ByteArray? {
        if (state.value.preparing) return null
        state.value = ExportUiState(preparing = true)
        return try {
            val data = collectData()
            val bytes = when (format) {
                ExportFormat.CSV -> exportService.csv(data)
                ExportFormat.PDF -> exportService.pdf(data)
            }
            state.value = ExportUiState(
                preparing = false,
                message = "Plik ${format.fileName} jest gotowy.",
            )
            bytes
        } catch (e: AppException) {
            state.value = ExportUiState(preparing = false, error = e.message)
            null
        } catch (e: Exception) {
            state.value = ExportUiState(
                preparing = false,
                error = e.message ?: "Nie udało się przygotować eksportu.",
            )
            null
        }
    }

    fun reportSaved(fileName: String) {
        state.value = state.value.copy(message = "Zapisano $fileName.")
    }

    fun reportError(message: String) {
        state.value = state.value.copy(error = message)
    }

    fun clearMessages() {
        state.value = state.value.copy(error = null, message = null)
    }

    private suspend fun collectData(): ExportData = supervisorScope {
        val patient = async { runCatching { repository.patient() }.getOrNull() }
        val resources = ResourceType.entries.map { type ->
            async { type to fetchAll(type) }
        }.awaitAll().toMap()
        val documents = async { fetchDocuments() }
        val images = async { fetchImages() }

        val sections = buildList {
            ResourceType.entries.forEach { type ->
                add(resourceSection(type, resources[type].orEmpty()))
            }
            add(documentsSection(documents.await()))
            add(imagesSection(images.await()))
        }
        ExportData(patient = patient.await(), sections = sections)
    }

    private suspend fun fetchAll(type: ResourceType): List<JsonObject> {
        val all = mutableListOf<JsonObject>()
        var page = 0
        while (page < MAX_PAGES) {
            val response = repository.list(type, page = page, size = PAGE_SIZE)
            all += response.content
            if (response.last || response.content.isEmpty()) break
            page += 1
        }
        return all
    }

    private suspend fun fetchDocuments(): List<DocumentDto> {
        val all = mutableListOf<DocumentDto>()
        var page = 0
        while (page < MAX_PAGES) {
            val response = repository.documents(page = page, size = PAGE_SIZE)
            all += response.content
            if (response.last || response.content.isEmpty()) break
            page += 1
        }
        return all
    }

    private suspend fun fetchImages(): List<ImageDto> {
        val all = mutableListOf<ImageDto>()
        var page = 0
        while (page < MAX_PAGES) {
            val response = repository.images(page = page, size = PAGE_SIZE)
            all += response.content
            if (response.last || response.content.isEmpty()) break
            page += 1
        }
        return all
    }

    private fun resourceSection(type: ResourceType, items: List<JsonObject>): ExportSection {
        val columns = COLUMNS.getValue(type)
        return ExportSection(
            title = type.title,
            columns = columns.map { it.second },
            rows = items.map { json -> columns.map { (key, _) -> value(json, key) } },
        )
    }

    private fun documentsSection(items: List<DocumentDto>): ExportSection = ExportSection(
        title = "Dokumenty",
        columns = listOf("Tytuł", "Typ", "Plik", "Data dokumentu", "Dodano", "Opis"),
        rows = items.map { document ->
            listOf(
                document.title,
                ResourceCatalog.prettyEnum(document.documentType),
                document.originalFilename,
                document.documentDate.orEmpty(),
                DateInputs.toDisplay(document.uploadedAt),
                document.description.orEmpty(),
            )
        },
    )

    private fun imagesSection(items: List<ImageDto>): ExportSection = ExportSection(
        title = "Zdjęcia",
        columns = listOf("Numer", "Tytuł", "Kategoria", "Plik", "Wykonano", "Dodano", "Opis"),
        rows = items.map { image ->
            listOf(
                image.imageNumber.toString(),
                image.title.orEmpty(),
                ResourceCatalog.prettyEnum(image.category),
                image.originalFilename,
                DateInputs.toDisplay(image.capturedAt),
                DateInputs.toDisplay(image.uploadedAt),
                image.description.orEmpty(),
            )
        },
    )

    private fun value(json: JsonObject, key: String): String {
        val element = json.get(key)?.takeUnless { it.isJsonNull } ?: return ""
        val raw = runCatching { element.asString }.getOrDefault("")
        if (raw.isBlank()) return ""
        return when (key) {
            "status", "severity", "type", "source", "category", "documentType", "origin" ->
                ResourceCatalog.prettyEnum(raw)
            "diagnosedAt", "onsetDate", "resolvedDate", "startDate", "endDate",
            "startedAt", "endedAt", "observedAt", "measuredAt", "scheduledAt",
            -> DateInputs.toDisplay(raw)
            else -> raw
        }
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 20

        val COLUMNS: Map<ResourceType, List<Pair<String, String>>> = mapOf(
            ResourceType.ALLERGIES to listOf(
                "substance" to "Substancja",
                "reaction" to "Reakcja",
                "severity" to "Nasilenie",
                "status" to "Status",
                "diagnosedAt" to "Data diagnozy",
                "notes" to "Notatki",
            ),
            ResourceType.CONDITIONS to listOf(
                "name" to "Nazwa",
                "description" to "Opis",
                "status" to "Status",
                "onsetDate" to "Początek",
                "resolvedDate" to "Koniec",
                "origin" to "Pochodzenie",
            ),
            ResourceType.MEDICATIONS to listOf(
                "name" to "Nazwa",
                "activeIngredient" to "Substancja czynna",
                "strength" to "Moc",
                "dosageForm" to "Postać",
                "prescribedDose" to "Dawka",
                "route" to "Droga",
                "frequency" to "Częstotliwość",
                "startDate" to "Od",
                "endDate" to "Do",
                "status" to "Status",
                "notes" to "Notatki",
            ),
            ResourceType.SYMPTOMS to listOf(
                "title" to "Tytuł",
                "description" to "Opis",
                "bodyLocation" to "Lokalizacja",
                "severity" to "Nasilenie",
                "startedAt" to "Początek",
                "endedAt" to "Koniec",
                "observedAt" to "Obserwacja",
                "status" to "Status",
            ),
            ResourceType.MEASUREMENTS to listOf(
                "type" to "Typ",
                "value" to "Wartość",
                "secondaryValue" to "Wartość dodatkowa",
                "unit" to "Jednostka",
                "measuredAt" to "Data",
                "source" to "Źródło",
                "notes" to "Notatki",
            ),
            ResourceType.DOCTORS to listOf(
                "firstName" to "Imię",
                "lastName" to "Nazwisko",
                "specialty" to "Specjalizacja",
                "facilityName" to "Placówka",
                "phone" to "Telefon",
                "email" to "Email",
                "address" to "Adres",
                "notes" to "Notatki",
            ),
            ResourceType.APPOINTMENTS to listOf(
                "scheduledAt" to "Termin",
                "endedAt" to "Koniec",
                "location" to "Miejsce",
                "reason" to "Powód",
                "status" to "Status",
                "notes" to "Notatki",
                "recommendations" to "Zalecenia",
            ),
        )
    }
}
