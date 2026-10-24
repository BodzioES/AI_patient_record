# Private AI Patient Card

Projekt studencki: prywatna karta pacjenta z lokalnym asystentem AI. Telefon Android
łączy się z backendem na PC w prywatnej sieci Wi-Fi. Dane medyczne, pliki i model AI
pozostają lokalnie.

**Aktualny stan: ETAP 2 ukończony** — backend Spring Boot z ETAPU 1 oraz aplikacja
Android (Jetpack Compose) z logowaniem, konfigurowalnym adresem backendu, profilem
pacjenta, CRUD wszystkich sekcji medycznych, uploadem/pobieraniem dokumentów i zdjęć,
historią rozmów asystenta oraz jawnym trybem MOCK AI.

> To nie jest produkt medyczny. Mock AI nie wykonuje rzeczywistej analizy obrazu ani nie
> udziela porad medycznych. Rzeczywisty model AI (np. LM Studio) zostanie podłączony
> w ETAPIE 4.

## 1. Technologie

| Warstwa | Technologia |
|---|---|
| Język | Kotlin 2.2.20 |
| Backend | Spring Boot 3.5.6, Spring Web, Security, Data JPA, Validation, Actuator |
| Baza | PostgreSQL 16, Flyway 11 |
| Build | Gradle 8.14.3 (Kotlin DSL), JDK 17 |
| Dokumentacja API | springdoc-openapi 2.8.13 (Swagger UI) |
| Tokeny | JWT HS256 (jjwt 0.12.6) + rotowane refresh tokeny |
| Testy | JUnit 5, Spring Boot Test, MockMvc, MockK, embedded PostgreSQL (zonky) |
| Android | Kotlin 2.0.21, Jetpack Compose (Material 3), Navigation Compose, ViewModel, Retrofit + Gson, OkHttp, coroutines, Android Keystore |

Wersje są zapisane w `backend/build.gradle.kts` i `backend/gradle/wrapper/gradle-wrapper.properties`.

## 2. Struktura repozytorium

```
/
  backend/                     # samodzielny build Spring Boot
    src/main/kotlin/com/patientcard/
      auth/                    # rejestracja, login, refresh, logout
      patient/                 # karta pacjenta
      allergy/ condition/ medication/ doctor/ appointment/ symptom/ measurement/
      document/ image/         # metadane + pliki
      storage/                 # bezpieczny magazyn lokalny
      ai/                      # API AI, kontekst pacjenta, MockAiProvider
      security/ config/ common/ repository/ entity/
    src/main/resources/
      db/migration/V1__initial_schema.sql
      application.yml application-dev.yml
    src/test/kotlin/com/patientcard/
    build.gradle.kts
  app/                         # aplikacja Android (Compose) — ETAP 2
    src/main/java/com/example/ar_project_1/
      data/                    # modele, API, sesja, repozytorium, upload, katalog formularzy
      ui/                      # welcome, auth, home, profil, zasoby, dokumenty, zdjęcia, asystent, ustawienia
      AppContainer.kt          # ręczne wstrzykiwanie zależności
    src/test/                  # testy jednostkowe JVM
  docs/                        # architektura, baza, API, AI, bezpieczeństwo, Android, roadmapa
  storage/                     # lokalne pliki (poza git, tylko .gitkeep)
  docker-compose.yml
  .env.example
  README.md
```

Backend jest osobnym buildem Gradle (`backend/settings.gradle.kts`), aby nie mieszać
go z buildem Androida.

## 3. Uruchomienie

### Wymagania

- JDK 17+
- PostgreSQL 16 (Docker Compose albo lokalna instalacja)
- opcjonalnie Docker Desktop

### 3.1. Wariant A (zalecany): baza + backend w Dockerze

```powershell
cd C:\Users\Student\AndroidStudioProjects\AR_PROJECT_1
Copy-Item .env.example .env
# zmień POSTGRES_PASSWORD i JWT_SECRET w .env
docker compose up -d --build
docker compose ps
```

Compose buduje obraz z `backend/Dockerfile` i uruchamia dwa kontenery:

- `patientcard-postgres` — PostgreSQL 16, wystawiony tylko na `127.0.0.1:5432`,
- `patientcard-backend` — Spring Boot na porcie `8080`, pliki w wolumenie
  `patientcard_storage`.

Pierwszy build trwa kilka minut (pobiera zależności Gradle). Gotowość sprawdzisz na
`http://localhost:8080/actuator/health` → `{"status":"UP"}`.

```powershell
docker compose logs -f backend   # logi backendu
docker compose down              # stop (dane w wolumenach zostają)
```

Wymagany jest Docker Desktop z działającym silnikiem (na Windows: WSL 2).

### 3.2. Wariant B: backend lokalnie z Gradle

Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd backend
.\gradlew.bat bootRun
```

Linux/macOS:

```bash
cd backend
JAVA_HOME=/path/to/jdk17 ./gradlew bootRun
```

Domyślne wartości są w `application.yml`. Warto ustawić co najmniej:

```powershell
$env:JWT_SECRET = "<losowy sekret >= 32 bajty>"
$env:STORAGE_ROOT = "C:\Users\Student\AndroidStudioProjects\AR_PROJECT_1\storage"
```

Po starcie:

- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

Flyway tworzy schemat przy pierwszym uruchomieniu. Hibernate ma
`ddl-auto: validate` — aplikacja nie zmienia schematu poza migracjami.

### 3.3. Baza bez Dockera

Utwórz bazę i użytkownika, np.:

```sql
CREATE USER patientcard WITH PASSWORD 'patientcard';
CREATE DATABASE patientcard OWNER patientcard;
```

Następnie ustaw `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` i uruchom backend.

## 4. Testy

```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat test
```

Testy integracyjne używają **prawdziwego PostgreSQL** uruchamianego przez
`io.zonky.test:embedded-postgres` — Docker nie jest wymagany. Flyway wykonuje
rzeczywistą migrację, a Hibernate waliduje mapowanie encji względem schematu.

Stan na tej sesji: **15 testów, 0 błędów**. Pełny build:

```powershell
.\gradlew.bat build
```

Raport HTML: `backend/build/reports/tests/test/index.html`.

### 4.1. Android — build i testy jednostkowe

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Stan na tej sesji: **31 testów JVM, 0 błędów**, `BUILD SUCCESSFUL`.
APK debug: `app/build/outputs/apk/debug/app-debug.apk`.
Raport: `app/build/reports/tests/testDebugUnitTest/index.html`.

Testy obejmują normalizację adresu serwera, wybór adresu backendu (emulator/telefon),
parser błędów API, konwersję dat oraz budowanie i walidację żądań CRUD. Testy
instrumentowane (`app/src/androidTest`) wymagają emulatora lub telefonu i nie były
uruchamiane w tym środowisku.

## 5. Aplikacja Android (ETAP 2)

### 5.1. Funkcje

- automatyczne wykrywanie adresu backendu (bez wpisywania IP i portu),
- ekran powitalny przy pierwszym uruchomieniu,
- logowanie i rejestracja, automatyczne odświeżanie tokenu przy `401`,
- szyfrowanie tokenów i adresu serwera kluczem AES-GCM z Android Keystore,
- profil pacjenta: podgląd i edycja,
- listy i formularze: alergie, schorzenia, leki, objawy, pomiary, lekarze, wizyty,
- dokumenty: upload (PDF/JPG/PNG), pobranie i otwarcie, usuwanie,
- zdjęcia: upload, podgląd w aplikacji, usuwanie, analiza MOCK,
- asystent AI: czat MOCK, historia rozmów, źródła, wyraźne oznaczenie MOCK,
- ustawienia: wylogowanie, czyszczenie pamięci podręcznej, informacje o aplikacji,
- komunikaty błędów po polsku, stany ładowania i puste.

### 5.2. Uruchomienie

1. Uruchom backend na PC (sekcja 3.1 — Docker — albo 3.2 — Gradle).
2. Podłącz telefon i przekieruj port: `adb reverse tcp:8080 tcp:8080`
   (emulator nie potrzebuje `adb reverse`).
3. Zbuduj i zainstaluj APK:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:installDebug
```

4. Otwórz aplikację — adres serwera ustawia się sam (`127.0.0.1:8080` na telefonie,
   `10.0.2.2:8080` w emulatorze). Nie ma pola do wpisywania adresu.
5. Pierwsze uruchomienie pokazuje ekran powitalny; „Kontynuuj” przechodzi do logowania.

Jeśli telefon ma łączyć się przez Wi-Fi zamiast `adb reverse`, musi być w tej samej
sieci co PC, a backend musi nasłuchiwać na interfejsie sieciowym. Automatyczne
wykrywanie sprawdza wtedy tylko `127.0.0.1` i `10.0.2.2`.

Szczegóły implementacji i ograniczenia: [`docs/android.md`](docs/android.md).

## 6. API — skrót

Wszystkie endpointy poza rejestracją, logowaniem, refresh, logout, Swaggerem i
health wymagają `Authorization: Bearer <accessToken>`.

### Auth — `/api/v1/app/auth`

| Metoda | Ścieżka | Opis |
|---|---|---|
| POST | `/register` | utworzenie konta i karty pacjenta, 201 |
| POST | `/login` | access + refresh token |
| POST | `/refresh` | rotacja refresh tokenu |
| POST | `/logout` | unieważnienie refresh tokenu |
| GET | `/me` | dane użytkownika i `patientId` |

### Dane aplikacji — `/api/v1/app`

`/patient`, `/allergies`, `/conditions`, `/medications`, `/medication-intake`,
`/doctors`, `/appointments`, `/symptoms`, `/measurements`, `/documents`, `/images`.

CRUD, paginacja (`page`, `size`, `sort`), filtrowanie tam, gdzie ma sens.
Pliki: `POST` multipart, `GET /{id}/file`, `DELETE`.

### AI — `/api/v1/ai`

| Metoda | Ścieżka | Opis |
|---|---|---|
| POST | `/chat` | rozmowa; ETAP 1 zwraca odpowiedź MOCK |
| GET | `/conversations` | historia rozmów |
| GET | `/conversations/{id}` | rozmowa z wiadomościami i źródłami |
| DELETE | `/conversations/{id}` | usunięcie rozmowy |
| POST | `/images/{imageId}/analyze` | zapis wyniku MOCK |
| GET | `/images/{imageId}/analysis` | wyniki analiz zdjęcia |

Pełna lista i przykłady: [`docs/api.md`](docs/api.md).

## 7. Bezpieczeństwo (minimum)

- hasła: BCrypt (strength 12), nigdy nie zwracane,
- access JWT 15 min, refresh 30 dni, tylko hash refresh tokenu w bazie,
- rotacja refresh tokenu i revokacja przy logout,
- właściciel zasobu zawsze z tokenu, nigdy z body,
- każde odczyty/edycja/usunięcie sprawdza `patient_id`,
- pliki poza katalogiem statycznym, serwowane kontrolowanie,
- walidacja rozszerzenia, MIME i sygnatury pliku, limit 25 MB,
- `storage_key` generowany serwerowo, ochrona przed path traversal,
- centralna obsługa błędów, bez stack trace'ów i bez danych medycznych w logach,
- Android: tokeny i adres serwera szyfrowane AES-GCM kluczem w Android Keystore,
  brak backupu danych aplikacji, `allowBackup=false`, po zmianie serwera sesja jest czyszczona.

Szczegóły i ograniczenia: [`docs/security.md`](docs/security.md).

## 8. Dokumentacja

- [`docs/architecture.md`](docs/architecture.md)
- [`docs/database.md`](docs/database.md)
- [`docs/database-erd.mmd`](docs/database-erd.mmd)
- [`docs/api.md`](docs/api.md)
- [`docs/ai-architecture.md`](docs/ai-architecture.md)
- [`docs/security.md`](docs/security.md)
- [`docs/development.md`](docs/development.md)
- [`docs/roadmap.md`](docs/roadmap.md)
- [`docs/android.md`](docs/android.md)

## 9. Podłączenie do nowego repozytorium GitHub

Git nie jest zainstalowany w środowisku, w którym powstał projekt. Na swoim PC:

```bash
cd C:\Users\Student\AndroidStudioProjects\AR_PROJECT_1
git init
git branch -M main
git add .
git commit -m "ETAP 1-2: backend karty pacjenta oraz aplikacja Android Compose"
git remote add origin https://github.com/TWOJ_LOGIN/TWOJE_REPO.git
git push -u origin main
```

Jeśli na GitHubie przy tworzeniu repo zaznaczono README/license, zrób najpierw:

```bash
git pull --rebase origin main
```

albo (świadomie, tylko dla pustego repo) `git push --force -u origin main`.

`.gitignore` wyklucza `.env`, `local.properties`, buildy, `storage/*` (poza
`.gitkeep`) oraz klucze. Przed commitem sprawdź `git status` i nigdy nie dodawaj
prawdziwych danych medycznych.

## 10. Znane ograniczenia ETAPU 2

- brak rzeczywistego modelu AI — `MockAiProvider` jawnie oznacza odpowiedzi; czat i
  analiza zdjęcia w aplikacji są oznaczone jako MOCK,
- brak OCR i rzeczywistej analizy obrazu,
- brak trybu offline i powiadomień,
- brak testów instrumentowanych UI uruchomionych na emulatorze/telefonie,
- `width_px` / `height_px` zdjęć nadal nie są odczytywane z plików po stronie backendu,
- lista rekordów pobiera pierwsze 100 pozycji bez paginacji w UI,
- brak automatycznego czyszczenia osieroconych plików po awarii dysku,
- access token pozostaje ważny do wygaśnięcia po logout (refresh jest revokowany),
- brak rate limitingu i 2FA,
- Docker/PostgreSQL nie były dostępne w środowisku budowania; uruchomienie backendu z
  Dockerem nie zostało tu przetestowane, natomiast testy użyły prawdziwego PostgreSQL
  embedded,
- `git` nie jest dostępny w środowisku — commit i push wykonuje użytkownik.

## 11. Następny etap

ETAP 3: dopracowanie doświadczenia pacjenta (chronologia, filtry, wykresy, eksport,
dostępność, testy UI/CI). ETAP 4: podłączenie lokalnego modelu AI (LM Studio,
`LmStudioAiProvider`). **Rzeczywisty model AI nie jest jeszcze potrzebny — będzie
wymagany dopiero na początku ETAPU 4.**
