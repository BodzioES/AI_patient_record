# Testy projektu – podsumowanie

## 1. Podsumowanie

| Moduł | Rodzaj | Pliki | Testy | Wynik (21.11.2026) |
|---|---|---|---|---|
| Aplikacja Android | jednostkowe | 5 | 30 | 30 / 30 ✅ |
| Backend | funkcjonalne (integracyjne) | 6 | 17 | 17 / 17 ✅ |
| Backend | jednostkowe | 1 | 2 | 2 / 2 ✅ |
| **Razem** | | **12** | **49** | **49 / 49 ✅** |
| Asystent AI (prawdziwy model) | ręczne | 1 scenariusz | 24 pytania + 9 zdjęć | wykonuje użytkownik |

**Rodzaje testów:**

- **Jednostkowe** sprawdzają pojedynczą klasę lub funkcję w izolacji, bez serwera,
  bazy danych i emulatora. Zależności są zastępowane atrapami (MockK). Wykonują się
  w kilka sekund.
- **Funkcjonalne (integracyjne)** uruchamiają cały backend Spring Boot z prawdziwą bazą
  PostgreSQL (embedded, w pamięci). Wysyłają zapytania HTTP tak jak aplikacja (MockMvc)
  i sprawdzają odpowiedzi oraz zawartość bazy. Flyway wykonuje prawdziwe migracje.
  Asystent AI działa w trybie MOCK.
- **Ręczne testy AI** sprawdzają jakość odpowiedzi prawdziwego modelu z LM Studio.
  Odpowiedzi modelu nie są powtarzalne, więc nie da się ich sprawdzać automatycznie.

## 2. Jak uruchomić i obejrzeć wyniki

Wszystkie komendy wpisujesz w PowerShellu w głównym folderze projektu.

**Aplikacja Android:**

```powershell
.\gradlew.bat :app:testDebugUnitTest
start app\build\reports\tests\testDebugUnitTest\index.html
```

**Backend** (wymaga JDK 17):

```powershell
cd backend
.\gradlew.bat test
start build\reports\tests\test\index.html
```

**W Android Studio:** prawy przycisk na pakiecie `com.example.ar_project_1 (test)`
→ **Run 'Tests in…'**. Backend to osobny projekt Gradle: otwórz folder `backend`
przez **File → Open**, a potem użyj zielonej strzałki przy klasie testowej.

Raport HTML pokazuje liczbę testów, czas i wynik. Po kliknięciu w klasę widać
poszczególne testy, a przy błędzie także jego treść.

---

## 3. Testy jednostkowe – aplikacja Android

Folder: `app/src/test/java/com/example/ar_project_1/data/`

### 3.1. `api/ServerUrlTest.kt` – adres serwera (7)

Sprawdza normalizację adresu backendu wpisywanego lub wykrywanego przez aplikację.

| Test | Co sprawdza |
|---|---|
| adds http scheme and api path | dopisuje `http://` i `/api/v1/` do gołego adresu |
| keeps https and replaces any path | zachowuje `https`, zamienia ścieżkę na `/api/v1/` |
| accepts already normalized url | poprawny adres zostaje bez zmian |
| trims whitespace | obcina spacje |
| rejects blank input | odrzuca pusty adres |
| rejects unsupported scheme | odrzuca protokoły inne niż http/https |
| display strips api path | na ekranie pokazuje adres bez `/api/v1` |

### 3.2. `api/ServerAutoConfigTest.kt` – wykrywanie backendu (3)

| Test | Co sprawdza |
|---|---|
| phone tries loopback first | telefon próbuje najpierw `127.0.0.1` (adb reverse) |
| emulator tries host alias first | emulator próbuje najpierw `10.0.2.2` (komputer) |
| candidates always contain both addresses | zawsze sprawdzane są oba adresy |

### 3.3. `api/ApiErrorParserTest.kt` – komunikaty błędów (5)

| Test | Co sprawdza |
|---|---|
| reads problem detail code and field errors | odczytuje kod błędu i błędy pól z odpowiedzi serwera |
| falls back to status message when body is empty | pusta odpowiedź daje komunikat zależny od kodu HTTP |
| falls back when body is not json | odpowiedź, która nie jest JSON-em, nie wywraca aplikacji |
| maps 401 to session message | błąd 401 daje komunikat o wygasłej sesji |
| network error has stable code | brak sieci ma zawsze ten sam kod błędu |

### 3.4. `resource/DateInputsTest.kt` – daty (7)

| Test | Co sprawdza |
|---|---|
| date only is converted to start of day instant | sama data zamienia się na początek dnia |
| date and time is converted using local zone | data z godziną jest liczona w lokalnej strefie czasowej |
| iso instant is preserved | data w formacie ISO zostaje bez zmian |
| invalid value throws readable error | błędna data daje czytelny komunikat |
| empty value is rejected | pusta wartość jest odrzucana |
| display and form value use the same local format | wyświetlanie i formularz używają tego samego formatu |
| unknown value is returned unchanged | nierozpoznana wartość nie jest psuta |

### 3.5. `resource/RecordRequestBuilderTest.kt` – formularze (8)

Sprawdza budowanie danych wysyłanych z formularzy (alergie, leki, pomiary itd.).

| Test | Co sprawdza |
|---|---|
| create omits empty optional fields | przy dodawaniu puste pola opcjonalne nie są wysyłane |
| update sends null for cleared optional fields | przy edycji wyczyszczone pole wysyła `null` (kasuje wartość) |
| missing required field is rejected | brak wymaganego pola jest wykrywany przed wysłaniem |
| decimal accepts comma separator | liczby akceptują przecinek, np. „37,4” |
| invalid date is rejected before request | błędna data jest odrzucana bez wysyłania zapytania |
| initial values use catalog defaults | nowy formularz ma poprawne wartości domyślne |
| prefill converts timestamps to form format | edycja wypełnia formularz istniejącymi danymi |
| required validation reports first missing field | komunikat wskazuje pierwsze brakujące pole |

---

## 4. Testy funkcjonalne – backend

Folder: `backend/src/test/kotlin/com/patientcard/`

Wspólna klasa bazowa `AbstractIntegrationTest.kt` uruchamia Spring Boot z bazą
embedded PostgreSQL i udostępnia pomocnicze metody (rejestracja, zapytania HTTP).

### 4.1. `AuthIntegrationTest.kt` – konta i logowanie (6)

| Test | Co sprawdza |
|---|---|
| register creates a patient card and returns tokens without exposing password hash | rejestracja tworzy konto i kartę pacjenta, zwraca tokeny, nie zwraca hasła |
| login works and wrong password is rejected | logowanie działa, złe hasło daje błąd |
| protected endpoints reject missing or invalid tokens | bez tokenu lub z fałszywym tokenem dostęp jest zablokowany (401) |
| refresh tokens rotate and logout revokes them | odświeżanie wydaje nowy token, po wylogowaniu stary przestaje działać |
| duplicate email and validation errors return proper statuses | zajęty e-mail i błędne dane zwracają poprawne kody błędów |
| user cannot use another user's patient id from request body | nie da się podać cudzego ID pacjenta i dostać się do jego danych |

### 4.2. `ClinicalDataIntegrationTest.kt` – dane medyczne (2)

| Test | Co sprawdza |
|---|---|
| full clinical data flow with isolation between patients | pełny CRUD: profil, alergie, schorzenia, leki, przyjęcia leków, lekarze, wizyty, objawy, pomiary; pacjent B nie widzi ani nie zmieni danych pacjenta A |
| invalid dates and enums are rejected | błędne daty (np. koniec przed początkiem) i nieznane wartości są odrzucane (400) |

### 4.3. `FileIntegrationTest.kt` – pliki (3)

| Test | Co sprawdza |
|---|---|
| images receive stable per-patient numbers and can be linked to a symptom | zdjęcia dostają kolejne numery dla każdego pacjenta, można je powiązać z objawem |
| documents upload download and delete with owner checks | dokumenty można wgrać, pobrać i usunąć tylko jako właściciel |
| storage is not exposed as public static content | pliki nie są dostępne przez publiczny link |

### 4.4. `ChatImageIntegrationTest.kt` – zdjęcia z Androida i czat (2)

| Test | Co sprawdza |
|---|---|
| android style uploads without extension or with alias mime are accepted | akceptuje zdjęcia z Androida bez rozszerzenia albo z ogólnym typem (`image/*`), rozpoznaje format po zawartości i odczytuje wymiary; odrzuca plik, którego zawartość nie zgadza się z rozszerzeniem (415) |
| chat accepts attached images and stores them with the user message | czat przyjmuje załączone zdjęcia i zapisuje je przy wiadomości |

### 4.5. `AiSkeletonIntegrationTest.kt` – asystent AI (2)

| Test | Co sprawdza |
|---|---|
| chat uses mock provider, saves history and validates sources | czat zapisuje historię i źródła danych, obca osoba nie widzi ani nie usunie cudzej rozmowy |
| image analysis is clearly marked as mock and stored as a separate record | analiza zdjęcia jest zapisywana jako osobny rekord i oznaczona jako MOCK |

### 4.6. `MigrationIntegrationTest.kt` – baza danych (2)

| Test | Co sprawdza |
|---|---|
| flyway migration creates the expected schema | migracje tworzą wszystkie tabele |
| image numbering constraints exist | istnieją ograniczenia, np. unikalny numer zdjęcia u pacjenta |

---

## 5. Testy jednostkowe – backend

### 5.1. `ai/context/DefaultPatientContextProviderTest.kt` – dane dla modelu AI (2)

Sprawdza klasę, która decyduje, jakie dane z karty pacjenta trafiają do modelu.

| Test | Co sprawdza |
|---|---|
| question about a drug by name always includes allergies | pytanie „Czy mogę wziąć ibuprofen?” dołącza listę alergii |
| measurement limit applies per type so one series does not push others out | seria pomiarów ciśnienia nie wypycha z danych pomiarów glukozy |

---

## 6. Ręczne testy asystenta AI

Scenariusz: [`scenariusz-testowy-ai.md`](scenariusz-testowy-ai.md)

Sprawdzają zachowanie prawdziwego modelu na fikcyjnym pacjencie (Jan Testowy):

- czy poprawnie odczytuje leki, alergie, choroby, wizyty i pomiary,
- czy reaguje na objawy alarmowe (112 / SOR),
- czy nie zmienia dawek i nie stawia pewnych diagnoz,
- czy ignoruje polecenia ukryte w danych (prompt injection),
- czy nie zmyśla treści dokumentów,
- czy poprawnie opisuje zdjęcia i odczytuje z nich tekst.

Przypadki krytyczne: ciśnienie 186/116, ibuprofen przy alergii, ból w klatce
piersiowej, polecenie ukryte w objawie, wartość LDL z PDF.

## 7. Czego testy nie obejmują

- interfejsu aplikacji Android (brak testów UI na emulatorze),
- jakości odpowiedzi modelu w testach automatycznych (używają trybu MOCK),
- eksportu CSV/PDF, wykresów, chronologii i wyszukiwania (brak osobnych testów),
- wydajności i obciążenia.
