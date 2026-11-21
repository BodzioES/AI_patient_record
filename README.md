# Private AI Patient Card

Projekt studencki: prywatna karta pacjenta z lokalnym asystentem AI. Aplikacja Android
łączy się z backendem na PC. Dane medyczne, pliki i model AI pozostają lokalnie.

**Stan: ETAP 4 ukończony.** Backend Spring Boot, aplikacja Android (Jetpack Compose)
z pełną kartą pacjenta, dokumentami, zdjęciami, chronologią, wyszukiwaniem, wykresami
i eksportem CSV/PDF oraz asystent AI na lokalnym modelu z LM Studio (czat i analiza
zdjęć). Domyślnie działa tryb MOCK; prawdziwy model włącza `AI_PROVIDER=lmstudio`.

> To nie jest produkt medyczny. Model może się mylić, a aplikacja zawsze pokazuje
> zastrzeżenie medyczne. Asystent może wstępnie interpretować wyniki (np. „145/95 mmHg
> to wartości podwyższone”), ale nie stawia rozpoznania i nie zmienia dawek leków.

## 1. Technologie

| Warstwa | Technologia |
|---|---|
| Backend | Kotlin 2.2, Spring Boot 3.5 (Web, Security, Data JPA, Validation, Actuator), JDK 17 |
| Baza | PostgreSQL 16, Flyway |
| API | REST, JWT HS256 + rotowane refresh tokeny, Swagger UI (springdoc) |
| AI | LM Studio (API zgodne z OpenAI) albo `MockAiProvider` |
| Android | Kotlin 2.0, Jetpack Compose (Material 3), Retrofit, OkHttp, coroutines, Android Keystore |
| Testy | JUnit 5, MockMvc, MockK, embedded PostgreSQL (zonky) |

## 2. Struktura repozytorium

```
backend/    Spring Boot (osobny build Gradle)
  src/main/kotlin/com/patientcard/
    auth/ patient/ allergy/ condition/ medication/ doctor/ appointment/
    symptom/ measurement/ document/ image/ timeline/ search/
    ai/          czat, analiza zdjęć, budowanie kontekstu pacjenta, dostawcy AI
    storage/     bezpieczny magazyn plików
    security/ config/ common/ repository/ entity/
  src/main/resources/db/migration/   migracje Flyway
  src/test/                          testy backendu
app/        aplikacja Android (Compose)
  src/main/java/com/example/ar_project_1/
    data/        API, modele, sesja, repozytorium, upload
    ui/          ekrany: logowanie, karta, dokumenty, zdjęcia, asystent, ustawienia...
  src/test/                          testy jednostkowe
docs/       architektura, baza, API, AI, bezpieczeństwo, Android, scenariusz testów AI
storage/    lokalne pliki pacjentów (poza git)
docker-compose.yml, .env.example
```

## 3. Uruchomienie

Wymagania: **Docker Desktop**, **Android Studio** z emulatorem, a do testów backendu
**JDK 17** (`winget install EclipseAdoptium.Temurin.17.JDK`).

### 3.1. Backend i baza (Docker)

```powershell
Copy-Item .env.example .env
# w .env zmień POSTGRES_PASSWORD, DB_PASSWORD i JWT_SECRET
docker compose up -d --build
```

Losowy `JWT_SECRET` wygenerujesz w PowerShellu:

```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Uruchamiają się dwa kontenery: `patientcard-postgres` (tylko `127.0.0.1:5432`)
i `patientcard-backend` (port `8080`, pliki w wolumenie `patientcard_storage`).
Pierwszy build trwa kilka minut.

| Adres | Co to jest |
|---|---|
| `http://localhost:8080/actuator/health` | stan backendu, oczekiwane `{"status":"UP"}` |
| `http://localhost:8080/swagger-ui.html` | dokumentacja i testowanie API |
| `http://localhost:8080/api/v1` | API |

```powershell
docker compose logs -f backend      # logi
docker compose up -d --build backend  # przebudowa po zmianach w kodzie
docker compose down                 # stop (dane zostają w wolumenach)
```

Backend bez Dockera: `cd backend; .\gradlew.bat bootRun`. Wymaga działającego
PostgreSQL i zmiennych `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`
(wartości domyślne są w `application.yml`).

### 3.2. Lokalny model AI (LM Studio)

1. W LM Studio załaduj model i włącz serwer (port 1234). Do analizy zdjęć potrzebny
   jest model z obsługą obrazu (vision), np. `qwen3-vl-30b-a3b-instruct`.
2. W `.env` ustaw:
   ```
   AI_PROVIDER=lmstudio
   LM_STUDIO_MODEL=<id z http://localhost:1234/v1/models>
   ```
3. `docker compose up -d backend`

Szczegóły: [`docs/development.md`](docs/development.md) (sekcja 11)
i [`docs/ai-architecture.md`](docs/ai-architecture.md).

### 3.3. Aplikacja Android

1. Uruchom backend (3.1).
2. Otwórz projekt w Android Studio i kliknij **Run** albo wpisz
   `.\gradlew.bat :app:installDebug`.
3. Adres serwera ustawia się sam: `10.0.2.2:8080` w emulatorze, `127.0.0.1:8080`
   na telefonie podłączonym kablem (po `adb reverse tcp:8080 tcp:8080`).

Pliki do wgrania na emulator możesz przeciągnąć na okno emulatora (trafią do
folderu Download).

## 4. Testy

### 4.1. Testy jednostkowe (Android)

```powershell
.\gradlew.bat :app:testDebugUnitTest
start app\build\reports\tests\testDebugUnitTest\index.html
```

### 4.2. Testy backendu (funkcjonalne i jednostkowe)

```powershell
cd backend
.\gradlew.bat test
start build\reports\tests\test\index.html
```

Testy uruchamiają **prawdziwy PostgreSQL w pamięci** (embedded), więc Docker nie jest
potrzebny. Flyway wykonuje migracje, a Hibernate sprawdza mapowanie encji.
Jeśli Gradle nie znajduje JDK 17, ustaw:
`$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17..."` (pełna nazwa
folderu).

Raport HTML pokazuje listę testów z wynikiem i czasem; po kliknięciu w klasę widać
poszczególne testy, a przy błędzie także jego treść.

| Zestaw | Testów | Co sprawdza |
|---|---|---|
| Android: `ServerUrlTest`, `ServerAutoConfigTest` | 10 | normalizacja i automatyczne wykrywanie adresu backendu |
| Android: `ApiErrorParserTest` | 5 | zamiana błędów API na komunikaty po polsku |
| Android: `DateInputsTest`, `RecordRequestBuilderTest` | 15 | parsowanie dat oraz budowanie i walidacja danych z formularzy |
| Backend: `AuthIntegrationTest` | 6 | rejestracja, logowanie, tokeny, dostęp do cudzych danych |
| Backend: `ClinicalDataIntegrationTest` | 2 | CRUD danych medycznych, izolacja pacjentów, walidacja |
| Backend: `FileIntegrationTest`, `ChatImageIntegrationTest` | 5 | pliki i zdjęcia: upload, pobieranie, uprawnienia, załączniki czatu |
| Backend: `AiSkeletonIntegrationTest` | 2 | czat AI, historia rozmów, źródła, analiza zdjęć |
| Backend: `DefaultPatientContextProviderTest` | 2 | jakie dane z karty trafiają do modelu AI |
| Backend: `MigrationIntegrationTest` | 2 | schemat bazy po migracjach |

Jakość odpowiedzi prawdziwego modelu testuje się ręcznie:
[`docs/scenariusz-testowy-ai.md`](docs/scenariusz-testowy-ai.md).

## 5. Funkcje aplikacji

- automatyczne wykrywanie backendu, logowanie i rejestracja, odświeżanie tokenu,
- tokeny i adres serwera szyfrowane AES-GCM kluczem z Android Keystore,
- profil pacjenta oraz listy i formularze: alergie, schorzenia, leki, objawy,
  pomiary, lekarze, wizyty (paginacja, sortowanie, filtr),
- chronologia zdarzeń i wyszukiwanie po całej karcie,
- wykresy pomiarów ze statystykami,
- eksport karty do CSV lub PDF,
- dokumenty (PDF/JPG/PNG) i zdjęcia: upload, podgląd, usuwanie, analiza AI zdjęć,
- asystent AI: czat z historią rozmów, źródłami i załącznikami zdjęć,
- ustawienia: rozmiar tekstu, wylogowanie, czyszczenie pamięci podręcznej.

Szczegóły: [`docs/android.md`](docs/android.md).

## 6. API (skrót)

Wszystko poza rejestracją, logowaniem, refresh, logout, Swaggerem i health wymaga
nagłówka `Authorization: Bearer <accessToken>`.

| Grupa | Ścieżki |
|---|---|
| Auth `/api/v1/app/auth` | `POST /register`, `/login`, `/refresh`, `/logout`; `GET /me` |
| Karta `/api/v1/app` | `/patient`, `/allergies`, `/conditions`, `/medications`, `/medication-intake`, `/doctors`, `/appointments`, `/symptoms`, `/measurements` (CRUD + paginacja) |
| Pliki `/api/v1/app` | `/documents`, `/images` (multipart `POST`, `GET /{id}/file`, `DELETE`) |
| Przegląd `/api/v1/app` | `GET /timeline`, `GET /search?q=` |
| AI `/api/v1/ai` | `GET /status`, `POST /chat`, `/conversations` (lista, szczegóły, usuwanie), `POST /images/{id}/analyze`, `GET /images/{id}/analysis` |

Pełna lista i przykłady: [`docs/api.md`](docs/api.md).

## 7. Bezpieczeństwo

- hasła BCrypt; access JWT 15 min, refresh 30 dni (w bazie tylko hash, rotacja,
  unieważnienie przy wylogowaniu),
- właściciel danych zawsze z tokenu, każdy odczyt i zapis sprawdza `patient_id`,
- pliki poza katalogiem publicznym, kontrola rozszerzenia, MIME i sygnatury, limit 25 MB,
  ochrona przed path traversal,
- błędy bez stack trace'ów i bez danych medycznych w logach,
- dane z karty przekazywane modelowi jako dane, nie polecenia (ochrona przed prompt injection).

Szczegóły: [`docs/security.md`](docs/security.md).

## 8. Znane ograniczenia

- brak OCR: model widzi tylko tytuł, typ i datę dokumentu, nie jego treść,
- odpowiedzi AI bez streamingu (na słabszym sprzęcie trzeba poczekać),
- analiza zdjęć wymaga modelu vision; zdjęcia powyżej 10 MB nie są wysyłane do modelu,
- pytanie uzupełniające w rozmowie nie przejmuje kategorii danych z poprzedniego pytania,
- wyszukiwanie oparte na `LIKE` (maks. 10 trafień na sekcję),
- brak trybu offline, powiadomień, rate limitingu i 2FA,
- access token pozostaje ważny do wygaśnięcia po wylogowaniu,
- brak testów UI na emulatorze.

## 9. Dokumentacja i dalsze etapy

[`architecture`](docs/architecture.md) ·
[`database`](docs/database.md) ·
[`api`](docs/api.md) ·
[`ai-architecture`](docs/ai-architecture.md) ·
[`security`](docs/security.md) ·
[`development`](docs/development.md) ·
[`android`](docs/android.md) ·
[`roadmap`](docs/roadmap.md) ·
[`scenariusz testów AI`](docs/scenariusz-testowy-ai.md)

Następny etap (ETAP 5): OCR dokumentów, kolejka zadań AI, wersjonowanie promptów.
