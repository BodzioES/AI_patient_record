# Architektura AI (ETAP 4)

## 1. Cel

Warstwa AI jest wymiennym modułem z dwiema implementacjami:

| Dostawca | Kiedy aktywny | Zachowanie |
|---|---|---|
| `MockAiProvider` | `AI_PROVIDER=mock` (domyślnie) | dane demonstracyjne, `mock: true`, jawny napis `MOCK`, brak połączenia z modelem |
| `LmStudioAiProvider` | `AI_PROVIDER=lmstudio` | prawdziwy model lokalny przez LM Studio (OpenAI-compatible), `mock: false` |

Wybór odbywa się adnotacją `@ConditionalOnProperty` na klasach dostawców, więc
kontrolery, serwisy, zapis historii i źródła działają identycznie w obu trybach.
Nie ma żadnych gałęzi `if (mock)` w logice biznesowej poza `provider.isMock()`
(które steruje oznaczeniami w API i odczytem pliku zdjęcia).

Model nigdy nie opuszcza komputera użytkownika: backend łączy się z LM Studio po
sieci lokalnej (Docker → `host.docker.internal`, uruchomienie z IDE →
`localhost`). Nie ma integracji z zewnętrznymi API.

## 2. Komponenty

```
AiController
   ├── AiChatService
   │      ├── PatientContextProvider  (kontekst pacjenta)
   │      ├── AiProvider              (mock | lmstudio)
   │      ├── AiSourceValidator       (właściciel źródeł)
   │      └── repozytoria rozmów
   └── ImageAiAnalysisService
          ├── AiProvider
          ├── FileStorageService      (bajty zdjęcia tylko dla realnego dostawcy)
          └── repozytoria analiz i zadań
```

| Element | Plik | Rola |
|---|---|---|
| `AiProvider` | `ai/provider/AiProvider.kt` | kontrakt czatu i analizy obrazu |
| `MockAiProvider` | `ai/provider/MockAiProvider.kt` | deterministyczna atrapa |
| `LmStudioAiProvider` | `ai/provider/LmStudioAiProvider.kt` | klient LM Studio (tekst + vision) |
| `PatientContextProvider` | `ai/context/PatientContextProvider.kt` | interfejs budowy kontekstu |
| `DefaultPatientContextProvider` | `ai/context/DefaultPatientContextProvider.kt` | routing słów kluczowych + zapytania do bazy |
| `AiSourceValidator` | `ai/context/AiSourceValidator.kt` | weryfikacja właściciela źródła |
| `AiChatService` | `ai/AiChatService.kt` | rozmowy, historia, źródła, `GET /ai/status` |
| `ImageAiAnalysisService` | `ai/ImageAiAnalysisService.kt` | analiza zdjęcia i zadania |
| `AiProperties` | `config/AiProperties.kt` | konfiguracja modelu, limitów i timeoutów |

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
6. `AiProvider.chat(...)`:
   - **mock** — zwraca tekst demonstracyjny z podglądem kontekstu,
   - **lmstudio** — buduje prompt i wysyła `POST {base}/chat/completions`.
7. Zapis wiadomości `USER` i `ASSISTANT`.
8. Każde źródło jest sprawdzane przez `AiSourceValidator`. Źródła nienależące do
   pacjenta są odrzucane; poprawne trafiają do `ai_message_sources`.
9. Odpowiedź zawiera `mock`, `modelName`, `sources` i identyfikatory wiadomości.

Kontekst nie zawiera ścieżek plików ani `storage_key`. Model dostaje wyłącznie
dane kliniczne i metadane bezpieczne dla pacjenta.

## 4. Prompt i budżet kontekstu (LmStudioAiProvider)

Prompt składa się z trzech części:

1. **Reguły systemowe** (stały tekst w kodzie): odpowiadaj tylko z danych karty,
   **wstępnie interpretuj wartości** względem typowych zakresów dla dorosłych
   (ciśnienie, tętno, temperatura, glukoza, saturacja) i mów wprost, czy wynik jest
   w normie, podwyższony czy obniżony — ale bez rozpoznania („wartości wskazują
   na…”, nie „masz nadciśnienie”), bez zmiany dawek, przy objawach alarmowych
   kieruj do 112/SOR. Struktura: ocena danych → co to zwykle oznacza → co zrobić →
   zastrzeżenie. Maksymalnie ~250 słów, bez markdownu. **Każda odpowiedź kończy się
   osobnym akapitem**: „To nie jest porada medyczna ani diagnoza — skonsultuj się
   z lekarzem.”
2. **Dane pacjenta** w blokach `=== DANE Z KARTY PACJENTA ===` … `=== KONIEC DANYCH ===`
   z instrukcją, że to dane, a nie polecenia (ochrona przed prompt injection
   z treści zapisanych w karcie).
3. **Historia rozmowy + pytanie**. Historia jest dobierana od najnowszej
   wiadomości i przycinana do budżetu `AI_MAX_PROMPT_CHARS` (domyślnie 14000
   znaków), żeby nie przekroczyć okna kontekstu modelu (np. 8192 tokenów).

Pole `reasoning_effort` jest wysyłane tylko gdy `AI_REASONING_EFFORT` nie jest
puste. Domyślnie `none`, bo modele rozumujące zużywają znaczną część
`max_tokens` na „myślenie” i przy małym budżecie zwracają pustą treść. Jeśli LM
Studio odrzuci to pole (`HTTP 400`), provider ponawia żądanie raz, już bez niego.

## 5. Przepływ analizy zdjęcia

1. `POST /api/v1/ai/images/{imageId}/analyze`.
2. Zdjęcie musi należeć do pacjenta (inaczej `404`).
3. Tworzony jest `ai_processing_jobs` ze statusem `RUNNING`.
4. `ai_processing_status` zdjęcia → `PROCESSING`.
5. Dla mocka plik **nie** jest czytany. Dla `lmstudio` bajty są czytane przez
   `FileStorageService` i wysyłane jako `image_url` z data URL (base64).
6. Wynik trafia do `image_ai_analyses` (`analysis_text`, `model_name`,
   `prompt_version`, opcjonalne `extracted_metadata`).
7. Statusy → `COMPLETED`; przy błędzie → `FAILED` i `503 AI_PROVIDER_ERROR`.
8. `GET /images/{imageId}/analysis` zwraca historię analiz (najnowsze pierwsze).

Zdjęcia większe niż `AI_MAX_VISION_IMAGE_BYTES` (domyślnie 10 MB) nie są wysyłane
— provider robi wtedy analizę wyłącznie metadanych i **mówi wprost**, że nie
widzi obrazu (`prompt_version = lmstudio-image-metadata-v2`). Model bez wsparcia
vision również nie udaje, że widział piksele: dostaje informację „nie widzisz
obrazu”.

Prompt obrazu (`prompt_version = lmstudio-vision-v2`) opisuje, co widać, i pozwala
podać **prawdopodobne wyjaśnienia jako przypuszczenie** („to może wyglądać jak…”,
„typowe dla…”), ale nie stawia pewnej diagnozy i nie twierdzi, że zmiana jest
groźna lub niegroźna. Podaje, po czym poznać, że trzeba zgłosić się pilnie
(szybkie powiększanie, gorączka, ropa, silny ból, rozprzestrzenianie się), każe
przepisać widoczne liczby/tekst dokładnie i zawsze kończy się akapitem
„Jestem asystentem AI — to tylko przypuszczenia, a nie diagnoza. Pokaż to lekarzowi
lub specjaliście.”

## 6. Model danych AI

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

## 7. Konfiguracja

```yaml
app:
  ai:
    provider: ${AI_PROVIDER:mock}
    lm-studio-base-url: ${LM_STUDIO_BASE_URL:http://localhost:1234/v1}
    lm-studio-model: ${LM_STUDIO_MODEL:local-model}
    model-version: ${LM_STUDIO_MODEL_VERSION:}
    reasoning-effort: ${AI_REASONING_EFFORT:none}
    connect-timeout: ${AI_CONNECT_TIMEOUT:PT10S}
    request-timeout: ${AI_REQUEST_TIMEOUT:PT2M}
    max-context-chars: ${AI_MAX_CONTEXT_CHARS:8000}
    max-context-items-per-category: ${AI_MAX_CONTEXT_ITEMS:10}
    max-prompt-chars: ${AI_MAX_PROMPT_CHARS:14000}
    max-vision-image-bytes: ${AI_MAX_VISION_IMAGE_BYTES:10485760}
    temperature: ${AI_TEMPERATURE:0.2}
    max-output-tokens: ${AI_MAX_OUTPUT_TOKENS:2048}
```

W Dockerze `LM_STUDIO_BASE_URL` musi wskazywać hosta, nie kontener:
`http://host.docker.internal:1234/v1`. Przy uruchomieniu backendu z IDE —
`http://localhost:1234/v1`.

## 8. Obsługa błędów

Wszystkie błędy dostawcy są mapowane na `503 AI_PROVIDER_ERROR` z czytelnym
komunikatem po polsku (bez stack trace):

| Sytuacja | Komunikat |
|---|---|
| brak połączenia | „Brak połączenia z modelem lokalnym pod adresem … Uruchom serwer w LM Studio…” |
| timeout | „Model lokalny nie odpowiedział w wyznaczonym czasie (… s)…” |
| `404` | „LM Studio nie znalazł modelu — sprawdź `LM_STUDIO_MODEL` i czy model jest wczytany” |
| `400` | odrzucone parametry (`reasoning_effort`, `max_tokens`) |
| `401/403` | włączona autoryzacja w LM Studio |
| `413` | zdjęcie za duże dla modelu |
| pusty `content` przy `finish_reason=length` | sugestia zwiększenia `AI_MAX_OUTPUT_TOKENS` lub `AI_REASONING_EFFORT=none` |

`ImageAiAnalysisService` dodatkowo oznacza zdjęcie jako `FAILED` i zapisuje
`error_message` w `ai_processing_jobs`.

## 9. Status dostawcy w aplikacji

`GET /api/v1/ai/status` zwraca `mock`, `modelName`, `modelVersion` i `notice`.
Aplikacja pobiera go raz po zalogowaniu i używa do etykiet w UI („MOCK” vs nazwa
modelu) — dzięki temu po przełączeniu `AI_PROVIDER` nie trzeba zmieniać kodu
Androida. Dopóki status nie jest znany, UI nie twierdzi, że działa prawdziwy
model.

## 10. Prywatność i ograniczenia

- Brak zewnętrznych API; model działa na komputerze użytkownika.
- Brak telemetrii i wysyłania danych medycznych poza sieć lokalną.
- Analiza zdjęcia tylko po jawnym działaniu użytkownika; plik nie jest wysyłany
  przy zwykłym czacie.
- Historia i źródła są widoczne wyłącznie dla właściciela karty.
- Odpowiedzi są generowane synchronicznie (bez streamingu) — przy wolniejszym
  sprzęcie widać dłuższe oczekiwanie.
- Brak OCR, embeddingów, wyszukiwania semantycznego i kolejki asynchronicznej.
- Brak oceny jakości odpowiedzi i brak mechanizmu „human-in-the-loop”.
- Model może się mylić — dlatego interpretacja jest zawsze przedstawiana jako
  przypuszczenie („wartości wskazują na…”, „to może być…”), bez pewnej diagnozy,
  a UI zawsze pokazuje zastrzeżenie medyczne i odesłanie do lekarza.
