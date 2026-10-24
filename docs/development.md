# Development

## 1. Wymagania

- JDK 17 (wystarczy JBR z Android Studio:
  `C:\Program Files\Android\Android Studio\jbr`).
- Gradle nie musi być zainstalowany — używamy wrappera `backend/gradlew.bat`.
- PostgreSQL 16: Docker Desktop albo lokalna instalacja.
- PowerShell lub terminal Linux/macOS.
- Opcjonalnie: Android Studio (ETAP 2).

Sprawdzenie JDK:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" -version
```

## 2. Konfiguracja środowiska

```powershell
cd C:\Users\Student\AndroidStudioProjects\AR_PROJECT_1
Copy-Item .env.example .env
```

W `.env` zmień co najmniej:

- `POSTGRES_PASSWORD` / `DB_PASSWORD`,
- `JWT_SECRET` (min. 32 bajty; np. `openssl rand -base64 48`).

Backend czyta zmienne środowiskowe; `.env` nie jest ładowany automatycznie przez
Spring Boot. Ustaw je w sesji PowerShell albo użyj narzędzia typu `direnv`.
Wartości domyślne w `application.yml` są tylko deweloperskie.

W PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:JWT_SECRET = "wstaw-losowy-sekret-min-32-bajty"
$env:DB_PASSWORD = "haslo-bazy"
$env:STORAGE_ROOT = "C:\Users\Student\AndroidStudioProjects\AR_PROJECT_1\storage"
```

## 3. Baza danych

### Docker Compose

```powershell
cd C:\Users\Student\AndroidStudioProjects\AR_PROJECT_1
docker compose up -d
docker compose ps
```

PostgreSQL jest wystawiony tylko na `127.0.0.1:5432`. Dane przetrwają w wolumenie
`patientcard_pgdata`. Zatrzymanie: `docker compose down` (bez `-v`, aby nie
usuwać danych).

### Bez Dockera

```sql
CREATE USER patientcard WITH PASSWORD 'patientcard';
CREATE DATABASE patientcard OWNER patientcard;
```

Następnie ustaw `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` i uruchom backend.

## 4. Uruchomienie backendu

```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat bootRun
```

Linux/macOS:

```bash
cd backend
JAVA_HOME=/path/to/jdk17 ./gradlew bootRun
```

Po starcie:

| Adres | Zawartość |
|---|---|
| `http://localhost:8080/api/v1` | API |
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON |
| `http://localhost:8080/actuator/health` | health check |

Flyway wykona migracje przy pierwszym starcie. Hibernate tylko waliduje schemat.

Profil `dev` (opcjonalnie):

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

Włącza on m.in. logowanie SQL. Nie używaj go w normalnym działaniu.

## 5. Testy

```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat test
```

Testy integracyjne:

- uruchamiają **prawdziwy PostgreSQL** przez `io.zonky.test:embedded-postgres`
  (pierwsze uruchomienie może pobrać binaria),
- wykonują rzeczywistą migrację Flyway,
- używają `MockMvc` i losowych danych fikcyjnych,
- czyszczą bazę i katalog plików przed każdym testem.

Zakres: auth i rotacja tokenów, migracja/schemat, izolacja danych klinicznych,
upload/pobieranie/usuwanie plików, numeracja zdjęć, mock AI i źródła.

Raport HTML: `backend/build/reports/tests/test/index.html`.

Pełny build (kompilacja, testy, jar):

```powershell
.\gradlew.bat build
```

Pojedyncza klasa:

```powershell
.\gradlew.bat test --tests "com.patientcard.AuthIntegrationTest"
```

## 6. Struktura backendu

```
backend/
  build.gradle.kts
  settings.gradle.kts
  gradlew.bat
  src/main/kotlin/com/patientcard/
    PatientCardApplication.kt
    auth/           rejestracja, login, refresh, logout
    patient/        karta pacjenta, CurrentPatientService
    allergy/ condition/ medication/ doctor/ appointment/ symptom/ measurement/
    document/ image/
    storage/        FileStorageService, FileTypeValidator
    ai/
      provider/     AiProvider, MockAiProvider
      context/      PatientContextProvider, walidacja źródeł
      AiChatService.kt
      ImageAiAnalysisService.kt
    security/       JwtService, filtr, principal
    config/         SecurityConfig, OpenApiConfig, properties
    common/         wyjątki, ProblemDetail, PageResponse, BaseEntity
    entity/         encje JPA
    repository/     repozytoria Spring Data
  src/main/resources/
    application.yml
    application-dev.yml
    db/migration/V1__initial_schema.sql
  src/test/kotlin/com/patientcard/
  src/test/resources/application-test.yml
```

Zasady:

- kontroler = mapowanie HTTP + walidacja DTO,
- serwis = transakcja, własność, reguły biznesowe,
- repozytorium = zapytania,
- brak `patientId` w DTO; zawsze z tokenu,
- błędy przez `ApiException` / centralny handler.

## 7. Migracje bazy

- Każda zmiana schematu to nowy plik `V2__opis.sql`, `V3__...`.
- **Nie edytuj migracji już zastosowanej** na trwałej bazie.
- `V1` wolno było poprawiać tylko dlatego, że nie istniała jeszcze baza
  produkcyjna.
- Po zmianie encji uruchom testy — Hibernate `validate` wykryje niezgodność.
- Nazwy tabel/kolumn: `snake_case`; encje mapują jawnie lub przez strategię
  Spring Boot.
- Nowe enumy: `VARCHAR` + `CHECK` w bazie i `@Enumerated(EnumType.STRING)`.

## 8. Dodanie nowego zasobu — checklista

1. Migracja tabeli (jeśli nowa tabela).
2. Encja w `entity/` z `patientId`.
3. Repozytorium z `findByIdAndPatientId`.
4. DTO request/response + walidacja.
5. Serwis z `CurrentPatientService` i sprawdzaniem powiązań.
6. Kontroler w `/api/v1/app/...`.
7. Test izolacji między pacjentami.
8. Dokumentacja w `docs/api.md`.

## 9. Rozwiązywanie problemów

| Objaw | Przyczyna / rozwiązanie |
|---|---|
| `JAVA_HOME is not set` | ustaw `JAVA_HOME` na JDK 17 |
| `JWT_SECRET must be at least 32 bytes` | ustaw dłuższy sekret |
| `Connection refused` do PostgreSQL | baza nie działa lub zły `DB_URL` |
| `Schema-validation: missing table` | nie wykonano migracji / zły profil |
| `Unsupported Media Type` przy uploadzie | zły MIME, rozszerzenie lub sygnatura |
| `413` przy uploadzie | przekroczony limit 25 MB |
| Testy nie startują | pierwszy start embedded PostgreSQL pobiera binaria; sprawdź sieć |
| `Port 8080 already in use` | zmień `SERVER_PORT` lub zatrzymaj proces |
| Brak tabeli `flyway_schema_history` | Flyway wyłączony lub zła baza |

## 10. Podłączenie do nowego repozytorium GitHub

W środowisku, w którym powstał projekt, `git` nie był dostępny. Wykonaj na
swoim PC, w katalogu projektu:

```bash
cd C:\Users\Student\AndroidStudioProjects\AR_PROJECT_1

git init
git branch -M main
git add .
git status
```

Sprawdź, że nie ma tam `.env`, `local.properties`, buildów ani plików medycznych.
Dopiero potem:

```bash
git commit -m "ETAP 1: backend karty pacjenta, PostgreSQL, auth i API AI (mock)"
git remote add origin https://github.com/TWOJ_LOGIN/TWOJE_REPO.git
git push -u origin main
```

Jeśli repozytorium na GitHubie zostało utworzone z README/license:

```bash
git pull --rebase origin main
git push -u origin main
```

`git push --force` tylko świadomie i tylko dla pustego repo.

Przydatne:

```bash
git remote -v
git log --oneline
git status
```

Nie commituj prawdziwych danych medycznych, sekretów, tokenów ani haseł.

## 11. Ograniczenia środowiska budowania

- `git`, `docker` i systemowy `psql` nie były dostępne, więc push na GitHub oraz
  live start przez Docker Compose nie zostały wykonane w tej sesji.
- Testy użyły prawdziwego PostgreSQL embedded, nie mocka bazy.
- Android nie jest częścią ETAPU 1.
