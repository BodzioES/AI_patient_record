# Private AI Patient Card

Projekt studencki: prywatna karta pacjenta z lokalnym asystentem AI. Telefon Android
łączy się z backendem na PC w prywatnej sieci Wi-Fi. Dane medyczne, pliki i model AI
pozostają lokalnie.

**Aktualny stan: ETAP 1 ukończony** — backend Spring Boot, PostgreSQL, Flyway,
logowanie JWT + refresh tokeny, API aplikacji, upload/pobieranie plików, szkielet API AI
z `MockAiProvider`, dokumentacja i testy integracyjne na prawdziwym PostgreSQL.

> To nie jest produkt medyczny. Mock AI nie wykonuje analizy obrazu ani nie udziela
> porad medycznych.

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
| Android | istniejący szablon Compose — implementacja w ETAPIE 2 |

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
  android/                     # planowane w ETAPIE 2 (obecnie szablon w /app)
  app/                         # istniejący szablon Android Studio
  docs/                        # architektura, baza, API, AI, bezpieczeństwo, roadmapa
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

### 3.1. Baza danych przez Docker Compose

```bash
cp .env.example .env
# zmień POSTGRES_PASSWORD i JWT_SECRET
docker compose up -d
docker compose ps
```

Baza jest wystawiona wyłącznie na `127.0.0.1:5432`.

### 3.2. Start backendu

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

## 5. API — skrót

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

## 6. Bezpieczeństwo (minimum)

- hasła: BCrypt (strength 12), nigdy nie zwracane,
- access JWT 15 min, refresh 30 dni, tylko hash refresh tokenu w bazie,
- rotacja refresh tokenu i revokacja przy logout,
- właściciel zasobu zawsze z tokenu, nigdy z body,
- każde odczyty/edycja/usunięcie sprawdza `patient_id`,
- pliki poza katalogiem statycznym, serwowane kontrolowanie,
- walidacja rozszerzenia, MIME i sygnatury pliku, limit 25 MB,
- `storage_key` generowany serwerowo, ochrona przed path traversal,
- centralna obsługa błędów, bez stack trace'ów i bez danych medycznych w logach.

Szczegóły i ograniczenia: [`docs/security.md`](docs/security.md).

## 7. Dokumentacja

- [`docs/architecture.md`](docs/architecture.md)
- [`docs/database.md`](docs/database.md)
- [`docs/database-erd.mmd`](docs/database-erd.mmd)
- [`docs/api.md`](docs/api.md)
- [`docs/ai-architecture.md`](docs/ai-architecture.md)
- [`docs/security.md`](docs/security.md)
- [`docs/development.md`](docs/development.md)
- [`docs/roadmap.md`](docs/roadmap.md)

## 8. Podłączenie do nowego repozytorium GitHub

Git nie jest zainstalowany w środowisku, w którym powstał projekt. Na swoim PC:

```bash
cd C:\Users\Student\AndroidStudioProjects\AR_PROJECT_1
git init
git branch -M main
git add .
git commit -m "ETAP 1: backend karty pacjenta, PostgreSQL, auth i API AI (mock)"
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

## 9. Znane ograniczenia ETAPU 1

- brak rzeczywistego modelu AI — `MockAiProvider` jawnie oznacza odpowiedzi,
- brak OCR i analizy obrazu,
- `width_px` / `height_px` nie są jeszcze odczytywane z plików,
- brak aplikacji Android (ETAP 2),
- brak automatycznego czyszczenia osieroconych plików po awarii dysku,
- access token pozostaje ważny do wygaśnięcia po logout (refresh jest revokowany),
- brak rate limitingu i 2FA,
- Docker/PostgreSQL nie były dostępne w środowisku budowania; uruchomienie
  backendu z Dockerem nie zostało tu przetestowane, natomiast testy użyły
  prawdziwego PostgreSQL embedded.

## 10. Następny etap

ETAP 2: aplikacja Android (Compose, logowanie, klient API, profil, listy, upload,
obsługa błędów sieci). **Nie rozpoczęty — czeka na akceptację.**
