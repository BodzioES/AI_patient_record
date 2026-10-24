# Aplikacja Android — ETAP 2

Klient Android prywatnej karty pacjenta. Aplikacja łączy się wyłącznie z backendem
wskazanym przez użytkownika (PC w prywatnej sieci Wi-Fi). Nie zawiera prawdziwych danych
medycznych ani sekretów.

> To projekt studencki, nie wyrób medyczny. Asystent AI i analiza zdjęcia działają
> w trybie **MOCK** — nie wykonują rzeczywistej analizy medycznej.

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
    session/                   # StoredSession, SessionManager, SecureSessionStore
    upload/                    # SAF -> RequestBody, zapis plików, FileProvider
  ui/
    MainApp.kt                 # trasy, dolna nawigacja, pasek tytułu
    auth/ setup/ home/ profile/
    resource/                  # generyczna lista + formularz dla 7 sekcji
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

## 4. Adres backendu

- Pole tekstowe przyjmuje `host:port`, `http://host:port` lub pełny URL.
- `ServerUrl.normalize` sprowadza wartość do `http(s)://host:port/api/v1/`.
- Adres jest zapisywany w zaszyfrowanej sesji.
- Zmiana adresu **czyści tokeny** — tokeny są związane z konkretnym serwerem.
- `ServerUrlInterceptor` przepisuje host/port każdego żądania, zachowując ścieżkę
  `/api/v1/...`; dzięki temu Retrofit ma stały `baseUrl`, a użytkownik może zmienić
  serwer bez restartu aplikacji.

Emulator: `http://10.0.2.2:8080`. Telefon: `http://<IP_PC>:8080`.

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
| `setup` | adres backendu (pierwsze uruchomienie / zmiana) |
| `auth` | logowanie i rejestracja |
| `home` | karta pacjenta, liczby wpisów, skróty |
| `profile`, `profile/edit` | podgląd i edycja profilu |
| `resource/{type}` | lista sekcji medycznej |
| `resource/{type}/new`, `resource/{type}/edit/{id}` | formularz |
| `documents` | lista, upload, pobranie, usunięcie |
| `images`, `images/{imageId}` | lista, upload, podgląd, analiza MOCK |
| `assistant` | czat MOCK, historia, źródła |
| `settings` | konto, serwer, cache, informacje |

Dolna nawigacja: Start, Dokumenty, Zdjęcia, Asystent, Ustawienia.

## 8. Tryb MOCK AI

- `POST ai/chat` jest wołane normalnie, ale backend odpowiada `MockAiProvider`.
- Odpowiedź ma flagę `mock`; UI pokazuje etykietę `MOCK` i ostrzeżenie, że to nie jest
  porada medyczna.
- `POST ai/images/{id}/analyze` zwraca `analysisText`, `mock`, `notice`; ekran zdjęcia
  wyświetla je w ramce „Analiza zdjęcia (MOCK)”.
- Brak streamingu, OCR i rzeczywistego modelu vision.

**Rzeczywisty model AI nie jest wymagany w ETAPIE 2/3.** Będzie potrzebny na początku
ETAPU 4, gdy powstanie `LmStudioAiProvider` i konfiguracja URL/modelu.

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

# instalacja na podłączonym urządzeniu/emulatorze
.\gradlew.bat :app:installDebug
```

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Raport testów: `app/build/reports/tests/testDebugUnitTest/index.html`
- Stan tej sesji: 28 testów JVM, 0 błędów, `BUILD SUCCESSFUL`.
- Testy instrumentowane (`app/src/androidTest`) wymagają emulatora/telefonu; nie były
  uruchamiane.

Testy jednostkowe nie wymagają Androida — obejmują czystą logikę (URL, daty, błędy API,
budowanie JSON). Klasy zależne od `Context`/Keystore nie mają testów JVM.

## 10. Rozwiązywanie problemów

| Objaw | Przyczyna / rozwiązanie |
|---|---|
| „Brak połączenia z serwerem” | zły IP/port, inna sieć Wi-Fi, backend nie działa, firewall Windows blokuje 8080 |
| Działa na emulatorze, nie na telefonie | użyj IP PC, nie `localhost`; zezwól Javie na ruch w zaporze |
| „Sesja wygasła” | refresh wygasł/unieważniony — zaloguj się ponownie |
| „Plik jest za duży” | limit 25 MB |
| Brak aplikacji do otwarcia pliku | zainstaluj przeglądarkę PDF / galerię |
| HTTPS nie działa | backend dev używa HTTP; konfiguracja cleartext jest tylko dla sieci prywatnej |

Nie używaj tego zestawu w internecie bez HTTPS, reverse proxy i utwardzenia z ETAPU 6.

## 11. Ograniczenia ETAPU 2

- brak paginacji w UI (pobierane jest pierwsze 100 rekordów),
- brak trybu offline, powiadomień, wykresów i eksportu,
- brak rzeczywistego AI/OCR,
- brak testów UI na urządzeniu,
- brak automatycznego odczytu wymiarów zdjęcia po stronie backendu,
- brak rate limitingu i 2FA po stronie serwera.
