# Architektura AI (ETAP 1)

## 1. Cel

Warstwa AI jest przygotowana jako wymienny moduł, ale w ETAPIE 1 **nie wykonuje
rzeczywistej analizy**. Dostępny jest wyłącznie `MockAiProvider`, który:

- nie wywołuje LM Studio ani żadnego modelu językowego,
- nie odczytuje pikseli zdjęcia,
- jawnie oznacza każdą odpowiedź słowem `MOCK`,
- zwraca `mock: true` w API,
- nie formułuje diagnozy ani zaleceń medycznych.

Celem etapu jest sprawdzenie przepływu: kontekst pacjenta → dostawca AI → zapis
historii i źródeł → odczyt przez API.

## 2. Komponenty

```
AiController
   ├── AiChatService
   │      ├── PatientContextProvider  (kontekst pacjenta)
   │      ├── AiProvider              (interfejs dostawcy)
   │      ├── AiSourceValidator       (właściciel źródeł)
   │      └── repozytoria rozmów
   └── ImageAiAnalysisService
          ├── AiProvider
          ├── FileStorageService      (tylko dla realnego dostawcy)
          └── repozytoria analiz i zadań
```

| Element | Plik | Rola |
|---|---|---|
| `AiProvider` | `ai/provider/AiProvider.kt` | kontrakt czatu i analizy obrazu |
| `MockAiProvider` | `ai/provider/MockAiProvider.kt` | deterministyczna atrapa |
| `PatientContextProvider` | `ai/context/PatientContextProvider.kt` | interfejs budowy kontekstu |
| `DefaultPatientContextProvider` | `ai/context/DefaultPatientContextProvider.kt` | routing słów kluczowych + zapytania do bazy |
| `AiSourceValidator` | `ai/context/AiSourceValidator.kt` | weryfikacja właściciela źródła |
| `AiChatService` | `ai/AiChatService.kt` | rozmowy, historia, źródła |
| `ImageAiAnalysisService` | `ai/ImageAiAnalysisService.kt` | analiza zdjęcia (mock) i zadania |

## 3. Przepływ czatu

1. `POST /api/v1/ai/chat` z `message` i opcjonalnym `conversationId`.
2. `CurrentPatientService` wyznacza `patientId` z JWT.
3. Jeśli brak `conversationId`, powstaje nowa rozmowa; inaczej wyszukiwanie
   `findByIdAndPatientId` (cudza rozmowa → `404`).
4. Pobierane jest ostatnie 20 wiadomości jako historia.
5. `DefaultPatientContextProvider`:
   - wykrywa kategorie po słowach kluczowych (alergie, leki, objawy, pomiary,
     wizyty, schorzenia, dokumenty, zdjęcia),
   - wykonuje precyzyjne zapytania po `patient_id`,
   - ogranicza liczbę elementów na kategorię (`AI_MAX_CONTEXT_ITEMS`, domyślnie 10),
   - ogranicza długość kontekstu (`AI_MAX_CONTEXT_CHARS`, domyślnie 8000),
   - zwraca listę źródeł `(type, id, label, relevance)`.
6. `AiProvider.chat(...)` zwraca odpowiedź (w ETAPIE 1 mock).
7. Zapis wiadomości `USER` i `ASSISTANT`.
8. Każde źródło jest sprawdzane przez `AiSourceValidator`. Źródła nienależące do
   pacjenta są odrzucane; poprawne trafiają do `ai_message_sources`.
9. Odpowiedź zawiera `mock`, `modelName`, `sources` i identyfikatory wiadomości.

Kontekst nie zawiera ścieżek plików ani `storage_key`. Model dostaje wyłącznie
dane kliniczne i metadane bezpieczne dla pacjenta.

## 4. Przepływ analizy zdjęcia

1. `POST /api/v1/ai/images/{imageId}/analyze`.
2. Zdjęcie musi należeć do pacjenta (inaczej `404`).
3. Tworzony jest `ai_processing_jobs` ze statusem `RUNNING`.
4. `ai_processing_status` zdjęcia → `PROCESSING`.
5. Dla mocka **nie** odczytywany jest plik; dla przyszłego dostawcy backend
   odczytałby bajty przez `FileStorageService`.
6. Wynik trafia do `image_ai_analyses` (`analysis_text`, `model_name`,
   `prompt_version`, opcjonalne `extracted_metadata`).
7. Statusy → `COMPLETED`; przy błędzie → `FAILED` i `503 AI_PROVIDER_ERROR`.
8. `GET /images/{imageId}/analysis` zwraca historię analiz (najnowsze pierwsze).

Mock nie udaje analizy: treść zaczyna się od `MOCK — nie wykonano analizy treści
zdjęcia.`, a pole `notice` wprost wskazuje `MockAiProvider`.

## 5. Model danych AI

| Tabela | Znaczenie |
|---|---|
| `chat_conversations` | rozmowa przypisana do pacjenta |
| `chat_messages` | wiadomości `USER` / `ASSISTANT` / `SYSTEM` |
| `ai_message_sources` | źródła odpowiedzi; `source_type` + `source_id` |
| `image_ai_analyses` | wyniki analiz zdjęć |
| `ai_processing_jobs` | ślad uruchomień AI (status, model, błąd) |
| `medical_document_extractions` | miejsce na OCR/ekstrakcję w przyszłości |

`ai_message_sources.source_id` jest polimorficzne, więc baza nie może wymusić FK.
Właściciel jest sprawdzany w `AiSourceValidator` dla typów: `IMAGE`, `DOCUMENT`,
`SYMPTOM`, `MEDICATION`, `APPOINTMENT`, `MEASUREMENT`, `CONDITION`.

## 6. Konfiguracja

```yaml
app:
  ai:
    provider: mock
    lm-studio-base-url: http://localhost:1234/v1
    lm-studio-model: local-model
    model-version:
    connect-timeout: PT10S
    request-timeout: PT2M
    max-context-chars: 8000
    max-context-items-per-category: 10
    temperature: 0.2
    max-output-tokens: 800
```

Zmienne środowiskowe: `AI_PROVIDER`, `LM_STUDIO_BASE_URL`, `LM_STUDIO_MODEL`,
`LM_STUDIO_MODEL_VERSION`, `AI_CONNECT_TIMEOUT`, `AI_REQUEST_TIMEOUT`,
`AI_MAX_CONTEXT_CHARS`, `AI_MAX_CONTEXT_ITEMS`, `AI_TEMPERATURE`,
`AI_MAX_OUTPUT_TOKENS`.

W ETAPIE 1 wartość `AI_PROVIDER` inna niż `mock` nie przełącza dostawcy —
`MockAiProvider` jest jedyną implementacją. Ustawienia LM Studio są przygotowane
pod ETAP 4 i nie są używane do połączeń sieciowych.

## 7. Docelowa integracja z LM Studio (ETAP 4)

Planowany `LmStudioAiProvider implements AiProvider`:

- OpenAI-compatible `POST /v1/chat/completions`,
- konfigurowalny URL i nazwa modelu, bez wpisywania IP na sztywno,
- timeouty i limit tokenów z `AiProperties`,
- obsługa błędów → `503 AI_PROVIDER_ERROR`, bez ujawniania stack trace,
- brak wysyłania plików, dopóki zadanie nie wymaga analizy obrazu,
- analiza obrazu tylko po jawnym działaniu użytkownika,
- nadal brak diagnozy i brak zastępowania konsultacji lekarskiej.

Wymiana dostawcy nie wymaga zmian w kontrolerach ani w logice zapisu historii.

## 8. Prywatność i ograniczenia

- Brak zewnętrznych API w ETAPIE 1.
- Brak telemetrii i wysyłania danych medycznych poza PC.
- Mock nie ma dostępu do plików.
- Historia i źródła są widoczne wyłącznie dla właściciela karty.
- Brak OCR, embeddingów, wyszukiwania semantycznego i kolejki asynchronicznej.
- Brak oceny jakości odpowiedzi i brak mechanizmu „human-in-the-loop”.
- `ai_processing_jobs` istnieje, ale przetwarzanie jest synchroniczne.
