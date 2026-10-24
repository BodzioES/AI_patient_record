# Aplikacja Android — ETAP 2

Klient Android prywatnej karty pacjenta. Aplikacja łączy się wyłącznie z backendem
wskazanym przez użytkownika (PC w prywatnej sieci Wi-Fi). Nie zawiera prawdziwych danych
medycznych ani sekretów.

> To projekt studencki, nie wyrób medyczny. Asystent AI i analiza zdjęcia działają
> z lokalnym modelem (LM Studio) albo w trybie **MOCK** — w żadnym z nich nie
> zastępują lekarza i nie stawiają diagnozy.

## 1. Stack

| Element | Wybór |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Nawigacja | Navigation Compose |
| Stan | ViewModel + `mutableStateOf` / `StateFlow` |
| HTTP | Retrofit 2 + Gson, OkHttp 4 |
| Współbieżność | Kotlin coroutines |
| Przechowywanie sesji | SharedPreferences + AES-GCM, klucz w Android Keystore |
| Min / target SDK | 29 / 36 |
| Build | AGP 9.0.1, Gradle 9.2.1, Kotlin 2.0.21 |

Moduł: `app/`, pakiet `com.example.ar_project_1` (pozostawiony szablon Android Studio).

## 2. Struktura kodu

```
app/src/main/java/com/example/ar_project_1/
  AppContainer.kt              # ręczne DI: sesja, API, repozytorium, FileSaver
  PatientCardApplication.kt
  MainActivity.kt
  data/
    api/                       # ApiService, ApiClient, interceptory, refresh, ServerUrl, błędy
    model/                     # DTO dopasowane do backendu
    repository/                # PatientCardRepository — jedyne wejście do API
    resource/                  # katalog pól formularzy, budowanie JSON, daty, podsumowania
    export/                    # ExportService — CSV i PDF budowane lokalnie
    session/                   # StoredSession, SessionManager, SecureSessionStore
    upload/                    # SAF -> RequestBody, zapis plików, FileProvider
  ui/
    MainApp.kt                 # trasy, dolna nawigacja, pasek tytułu, skala tekstu
    auth/ onboarding/ home/ profile/
    resource/                  # generyczna lista + formularz dla 7 sekcji
    timeline/                  # wspólna chronologia zdarzeń
    search/                    # wyszukiwanie po całej karcie
    charts/                    # wykresy pomiarów (Canvas)
    documents/ images/ assistant/ settings/ common/ theme/
```

## 3. Przepływ danych

1. Ekran wywołuje metodę ViewModelu.
2. ViewModel woła `PatientCardRepository` w `viewModelScope`.
3. Repozytorium mapuje wyjątki na `AppException` z komunikatem po polsku.
4. ViewModel publikuje stan (`loading`, `error`, dane); Compose przerysowuje ekran.

Siedem sekcji medycznych (alergie, schorzenia, leki, objawy, pomiary, lekarze, wizyty)
korzysta z jednego ekranu listy i formularza. Różnice opisuje `ResourceCatalog`
(etykiety, typy pól, listy wartości, sposób prezentacji). Żądania buduje
`RecordRequestBuilder`, który waliduje wymagane pola, liczby i daty.

## 4. Adres backendu (automatyczny)

- Użytkownik **nie wpisuje** adresu serwera — nie ma już ekranu `setup` ani sekcji
  „Serwer” w Ustawieniach.
- `ServerAutoConfig` przy starcie sprawdza `GET /actuator/health` na liście kandydatów:
  - emulator: `http://10.0.2.2:8080`, potem `http://127.0.0.1:8080`,
  - telefon: `http://127.0.0.1:8080` (przez `adb reverse`), potem `http://10.0.2.2:8080`.
- Wygrywa pierwszy adres, który odpowie; jest zapisywany w zaszyfrowanej sesji.
- Jeśli zapisany adres nadal odpowiada, nie jest zmieniany (tokeny zostają).
  Gdy przestanie odpowiadać (np. stary port), aplikacja podmienia go na wykryty.
- Gdy żaden adres nie odpowiada, zostaje pierwszy kandydat, a użytkownik widzi
  normalny błąd połączenia przy logowaniu — start aplikacji nie jest blokowany.
- `ServerUrl.normalize` sprowadza wartość do `http(s)://host:port/api/v1/`.
- Zmiana adresu **czyści tokeny** — tokeny są związane z konkretnym serwerem.
- `ServerUrlInterceptor` przepisuje host/port każdego żądania, zachowując ścieżkę
  `/api/v1/...`; dzięki temu Retrofit ma stały `baseUrl`, a adres może się zmienić
  bez restartu aplikacji.

Emulator: `http://10.0.2.2:8080`. Telefon: `adb reverse tcp:8080 tcp:8080` i
`http://127.0.0.1:8080`.

## 5. Uwierzytelnianie i sesja

- `POST app/auth/login` / `register` zwraca access + refresh token.
- `SessionManager` zapisuje sesję przez `SecureSessionStore`.
- `SecureSessionStore` szyfruje każdą wartość AES-GCM (klucz 256 bit w Android
  Keystore, alias `patient_card_session_aes`). Brak plaintextu na dysku.
- `AuthInterceptor` dodaje `Authorization: Bearer ...` poza endpointami auth.
- `TokenAuthenticator` przy `401` wykonuje jednorazowy refresh (blokująco na wątku
  OkHttp, osobnym klientem bez authenticatora), zapisuje nowe tokeny i powtarza żądanie.
- Nieudany refresh lub brak refresh tokenu czyści auth; `MainApp` przenosi użytkownika
  na ekran logowania.
- `allowBackup=false`; dane aplikacji nie trafiają do backupu.

## 6. Pliki

- Wybór pliku: SAF (`ActivityResultContracts.OpenDocument`), bez uprawnień do całej
  pamięci.
- `ContentUriRequestBody` strumieniuje `content://` do multipart, więc duże pliki nie
  są w całości w pamięci.
- Limit klienta i serwera: 25 MB.
- Pobieranie: `GET app/documents/{id}/file` lub `app/images/{id}/file`, zapis do
  `cacheDir/shared/`, otwarcie przez `FileProvider` (`${applicationId}.fileprovider`).
- Ustawienia pozwalają wyczyścić pobrane pliki.

## 7. Ekrany i trasy

| Trasa | Ekran |
|---|---|
| `welcome` | ekran powitalny (tylko przy pierwszym uruchomieniu) |
| `auth` | logowanie i rejestracja |
| `home` | karta pacjenta, liczby wpisów, skróty |
| `profile`, `profile/edit` | podgląd i edycja profilu |
| `resource/{type}` | lista sekcji medycznej |
| `resource/{type}/new`, `resource/{type}/edit/{id}` | formularz |
| `documents` | lista z sortowaniem i doładowywaniem, upload, pobranie, usunięcie |
| `images`, `images/{imageId}` | lista z filtrem kategorii i seriami, upload, podgląd, analiza AI |
| `assistant` | czat z modelem lokalnym lub MOCK, historia, źródła |
| `timeline` | wspólna chronologia z filtrem rodzaju i doładowywaniem |
| `charts` | wykresy pomiarów i statystyki |
| `search` | wyszukiwanie po całej karcie (ikona lupy w pasku tytułu) |
| `settings` | konto, eksport CSV/PDF, rozmiar tekstu, cache, informacje |

Dolna nawigacja (bez podpisów): Dokumenty, Asystent, **Start**, Zdjęcia, Ustawienia.

Ekran powitalny (`UiPreferences.welcomeSeen`) pokazuje się tylko raz, przed
logowaniem; „Kontynuuj” zapisuje flagę i przechodzi do logowania lub karty pacjenta.

### 7.1. Chronologia, wyszukiwanie i wykresy

- `timeline` pobiera `GET app/timeline` po 20 zdarzeń; „Pokaż więcej” doładowuje
  kolejne strony. Filtr rodzaju wysyła `types=...` do backendu.
- `search` odpytuje `GET app/search` po 350 ms od ostatniej zmiany tekstu; minimum
  to 2 znaki. Trafienie otwiera właściwą sekcję (albo Dokumenty/Zdjęcia).
- `charts` pobiera do 100 najnowszych pomiarów danego typu (`measuredAt,desc`),
  odwraca je do rosnącej osi czasu i rysuje własnym `Canvas` (bez biblioteki
  wykresów). Dla ciśnienia druga linia to wartości rozkurczowe.
- listy sekcji medycznych i dokumenty pobierają po 50 pozycji, mają sortowanie
  serwerowe (`sort=pole,kierunek`) oraz „Pokaż więcej”; listy sekcji mają dodatkowo
  lokalny filtr tekstowy na pobranych wpisach.

### 7.2. Eksport CSV/PDF

- `ExportViewModel` zbiera wszystkie sekcje partiami po 100 (limit backendu),
  maksymalnie 20 stron na sekcję.
- `ExportService` buduje CSV (BOM UTF-8, separator `;`, zgodny z Excelem) oraz PDF
  A4 przez `android.graphics.pdf.PdfDocument` (zawijanie tekstu, numeracja stron).
- Zapis przez SAF (`ActivityResultContracts.CreateDocument`), więc aplikacja nie
  potrzebuje uprawnień do całej pamięci. Plik nie opuszcza telefonu.

### 7.3. Dostępność

- Ustawienia → „Dostępność” zmienia skalę tekstu (0,9–1,4). Wartość jest zapisana
  w `UiPreferences`, a `MainApp` dostarcza ją przez `LocalDensity`.
- Motyw pozostaje jasny, szaro-biały z czerwonym akcentem; tryb ciemny nie jest
  częścią projektu.

## 8. Tryb AI (MOCK albo model lokalny)

- `POST ai/chat` jest wołane zawsze tak samo; backend wybiera dostawcę po
  `AI_PROVIDER` (`mock` lub `lmstudio`).
- `GET ai/status` zwraca `mock`, `modelName` i `notice`. Aplikacja pobiera status raz
  po zalogowaniu (`LocalAiStatus`) i na tej podstawie pokazuje etykietę `MOCK` albo
  nazwę modelu — w banerze czatu, na kafelku na starcie, w ustawieniach i na ekranie
  zdjęcia.
- Odpowiedź czatu ma flagę `mock`; przy prawdziwym modelu UI pokazuje jego nazwę
  i nadal ostrzega, że to nie jest porada medyczna.
- `POST ai/images/{id}/analyze` zwraca `analysisText`, `mock`, `notice`; ekran zdjęcia
  wyświetla je w ramce „Analiza zdjęcia” (z dopiskiem `(MOCK)` tylko w trybie mock).
- Dopóki status nie jest znany (brak połączenia), UI nie twierdzi, że działa
  prawdziwy model.
- Brak streamingu; klient HTTP ma zwiększony timeout odczytu (180 s), bo model
  generuje całą odpowiedź przed wysłaniem.

Podłączenie modelu: [`development.md`](development.md) (sekcja 11).

## 9. Build, testy, instalacja

Backend najprościej uruchomić z katalogu głównego projektu:

```powershell
docker compose up -d --build   # PostgreSQL + backend na :8080
docker compose ps
```

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# build + testy JVM
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest

# sam build i lint (ETAP 3)
.\gradlew.bat :app:assembleDebug :app:lintDebug

# instalacja na podłączonym urządzeniu/emulatorze
.\gradlew.bat :app:installDebug
```

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Raport testów: `app/build/reports/tests/testDebugUnitTest/index.html`
- Raport lint: `app/build/reports/lint-results-debug.html`
- Stan po ETAPIE 2: 31 testów JVM, 0 błędów, `BUILD SUCCESSFUL`.
- ETAP 3: `assembleDebug` i `lintDebug` zakończone sukcesem; **testy jednostkowe
  ETAPU 3 nie były uruchamiane** — użytkownik uruchamia je samodzielnie.
- Testy instrumentowane (`app/src/androidTest`) wymagają emulatora/telefonu; nie były
  uruchamiane.

Testy jednostkowe nie wymagają Androida — obejmują czystą logikę (URL, daty, błędy API,
budowanie JSON). Klasy zależne od `Context`/Keystore nie mają testów JVM.

## 10. Rozwiązywanie problemów

| Objaw | Przyczyna / rozwiązanie |
|---|---|
| „Brak połączenia z serwerem” | backend nie działa, brak `adb reverse tcp:8080 tcp:8080`, firewall Windows blokuje 8080 |
| Działa na emulatorze, nie na telefonie | brak `adb reverse`; ewentualnie użyj Wi-Fi i IP PC oraz zezwól Javie na ruch w zaporze |
| „Sesja wygasła” | refresh wygasł/unieważniony — zaloguj się ponownie |
| „Plik jest za duży” | limit 25 MB |
| Brak aplikacji do otwarcia pliku | zainstaluj przeglądarkę PDF / galerię |
| HTTPS nie działa | backend dev używa HTTP; konfiguracja cleartext jest tylko dla sieci prywatnej |

Nie używaj tego zestawu w internecie bez HTTPS, reverse proxy i utwardzenia z ETAPU 6.

## 11. Ograniczenia ETAPU 2 i 3

- brak trybu offline, powiadomień i trybu ciemnego (motyw ma pozostać jasny),
- brak rzeczywistego AI/OCR,
- brak testów UI na urządzeniu,
- brak automatycznego odczytu wymiarów zdjęcia po stronie backendu,
- brak rate limitingu i 2FA po stronie serwera,
- chronologia scala maks. 200 najnowszych pozycji z każdego źródła,
- wyszukiwanie opiera się na `LIKE` (bez indeksu pełnotekstowego) i zwraca maks.
  10 trafień na sekcję,
- filtr tekstowy na listach działa lokalnie na pobranych stronach,
- eksport ogranicza się do 20 stron po 100 pozycji na sekcję,
- testy jednostkowe ETAPU 3 pozostają do uruchomienia przez użytkownika.
