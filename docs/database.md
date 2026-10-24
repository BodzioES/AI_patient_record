# Baza danych

Schemat jest tworzony wyłącznie przez migrację
`backend/src/main/resources/db/migration/V1__initial_schema.sql` (Flyway).
Encje JPA są walidowane przy starcie (`ddl-auto: validate`) i nie zmieniają schematu.

Diagram: [`database-erd.mmd`](database-erd.mmd).

## 1. Konwencje

- Klucze główne: `UUID`, generowane przez aplikację (Hibernate); baza ma
  `DEFAULT gen_random_uuid()` jako zabezpieczenie.
- Znaczniki czasu: `TIMESTAMPTZ`, zapis w UTC; `created_at` / `updated_at` tam,
  gdzie obiekt jest edytowalny.
- Tabele historii (`medication_intake_logs`, `measurements`, `chat_messages`,
  `ai_message_sources`, `image_ai_analyses`, `refresh_tokens`) mają tylko
  `created_at`.
- Wartości enum: `VARCHAR` + `CHECK`, mapowane w Kotlinie przez
  `@Enumerated(EnumType.STRING)`.
- Wszystkie tabele danych medycznych mają `patient_id` z `ON DELETE CASCADE`.
- Daty bez czasu (`DATE`) dotyczą faktów kalendarzowych; zdarzenia i pomiary
  używają `TIMESTAMPTZ`.
- `ON DELETE SET NULL` stosowano dla opcjonalnych powiązań (lekarz, dokument,
  objaw), aby nie usuwać historii pacjenta razem z metadanym powiązaniem.

## 2. Tabele

### 2.1. `users`

Konta logowania.

| Kolumna | Typ | Null | Domyślnie | Opis |
|---|---|---|---|---|
| `id` | UUID | nie | `gen_random_uuid()` | PK |
| `email` | VARCHAR(320) | nie | – | unikalny, normalizowany do lowercase |
| `password_hash` | VARCHAR(100) | nie | – | BCrypt |
| `display_name` | VARCHAR(120) | nie | – | nazwa wyświetlana |
| `enabled` | BOOLEAN | nie | `true` | blokada konta |
| `created_at` | TIMESTAMPTZ | nie | `now()` | |
| `updated_at` | TIMESTAMPTZ | nie | `now()` | |

Ograniczenia: `uk_users_email`, `ck_users_email_not_blank`,
`ck_users_display_name_not_blank`.

### 2.2. `patient_profiles`

Główna karta pacjenta. W ETAPIE 1 dokładnie jedna karta na użytkownika.

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `user_id` | UUID | nie | FK -> `users.id`, unikalny, `ON DELETE CASCADE` |
| `first_name` | VARCHAR(100) | tak | |
| `last_name` | VARCHAR(100) | tak | |
| `date_of_birth` | DATE | tak | |
| `sex` | VARCHAR(16) | tak | `FEMALE`, `MALE`, `OTHER`, `UNKNOWN` |
| `height_cm` | NUMERIC(5,1) | tak | 0 < x < 300 |
| `weight_kg` | NUMERIC(5,1) | tak | 0 < x < 1000 |
| `blood_type` | VARCHAR(16) | tak | `A_POSITIVE` … `ZERO_NEGATIVE`, `UNKNOWN` |
| `notes` | VARCHAR(10000) | tak | |
| `created_at`, `updated_at` | TIMESTAMPTZ | nie | |

Ograniczenia: `uk_patient_profiles_user`, `ck_patient_height`,
`ck_patient_weight`, `ck_patient_sex`, `ck_patient_blood_type`.

### 2.3. `patient_image_counters`

Licznik numerów zdjęć w obrębie pacjenta.

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `patient_id` | UUID | nie | PK i FK -> `patient_profiles.id`, `ON DELETE CASCADE` |
| `last_image_number` | BIGINT | nie | ostatnio przydzielony numer, domyślnie 0 |

Ograniczenie: `ck_image_counter_non_negative`.

### 2.4. `doctors`

Lekarze zapisani przez pacjenta.

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `patient_id` | UUID | nie | FK -> `patient_profiles.id`, CASCADE |
| `first_name`, `last_name` | VARCHAR(100) | nie | |
| `specialty` | VARCHAR(120) | tak | |
| `facility_name` | VARCHAR(200) | tak | |
| `phone` | VARCHAR(50) | tak | |
| `email` | VARCHAR(320) | tak | |
| `address` | VARCHAR(500) | tak | |
| `notes` | VARCHAR(10000) | tak | |
| `created_at`, `updated_at` | TIMESTAMPTZ | nie | |

Indeksy: `idx_doctors_patient`, `idx_doctors_patient_name`.
Ograniczenie: `ck_doctors_names_not_blank`.

### 2.5. `medical_documents`

Metadane plików dokumentacji medycznej. Plik nie jest przechowywany w bazie.

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `patient_id` | UUID | nie | FK -> `patient_profiles.id`, CASCADE |
| `document_type` | VARCHAR(40) | nie | np. `LAB_RESULT`, `REFERRAL`; przechowywane uppercase |
| `title` | VARCHAR(200) | nie | |
| `description` | VARCHAR(10000) | tak | |
| `original_filename` | VARCHAR(255) | nie | tylko metadana, nigdy ścieżka |
| `storage_key` | VARCHAR(500) | nie | unikalny, generowany przez serwer |
| `mime_type` | VARCHAR(150) | nie | wykryty z sygnatury pliku |
| `file_size_bytes` | BIGINT | nie | > 0 |
| `sha256_checksum` | VARCHAR(64) | nie | hex lowercase |
| `document_date` | DATE | tak | data dokumentu, nie uploadu |
| `uploaded_at` | TIMESTAMPTZ | nie | czas zapisu na serwerze |
| `processing_status` | VARCHAR(32) | nie | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `NOT_REQUIRED` |
| `created_at`, `updated_at` | TIMESTAMPTZ | nie | |

Ograniczenia: `uk_documents_storage_key`, `ck_documents_size`,
`ck_documents_checksum`, `ck_documents_processing_status`.
Indeksy: `(patient_id, uploaded_at DESC)`, `(patient_id, document_type)`.

### 2.6. `medical_conditions`

Schorzenia i rozpoznania.

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `patient_id` | UUID | nie | FK -> `patient_profiles.id`, CASCADE |
| `name` | VARCHAR(200) | nie | |
| `description` | VARCHAR(10000) | tak | |
| `status` | VARCHAR(32) | nie | `ACTIVE`, `IN_REMISSION`, `RESOLVED`, `SUSPECTED` |
| `onset_date`, `resolved_date` | DATE | tak | `resolved_date >= onset_date` |
| `source_document_id` | UUID | tak | FK -> `medical_documents.id`, SET NULL |
| `origin` | VARCHAR(32) | nie | `PATIENT_REPORTED`, `CLINICIAN_DOCUMENTED`, `IMPORTED` |
| `created_at`, `updated_at` | TIMESTAMPTZ | nie | |

`origin` rozróżnia informację zgłoszoną przez pacjenta od udokumentowanego
rozpoznania lekarza. Indeksy: `(patient_id, status)`, `source_document_id`.

### 2.7. `allergies`

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `patient_id` | UUID | nie | FK -> `patient_profiles.id`, CASCADE |
| `substance` | VARCHAR(200) | nie | |
| `reaction` | VARCHAR(10000) | tak | |
| `severity` | VARCHAR(32) | nie | `MILD`, `MODERATE`, `SEVERE`, `LIFE_THREATENING`, `UNKNOWN` |
| `status` | VARCHAR(32) | nie | `ACTIVE`, `RESOLVED`, `SUSPECTED` |
| `diagnosed_at` | DATE | tak | |
| `notes` | VARCHAR(10000) | tak | |
| `created_at`, `updated_at` | TIMESTAMPTZ | nie | |

Indeks: `idx_allergies_patient_status`.

### 2.8. `symptoms`

Historia obserwacji objawów. Nowa obserwacja = nowy rekord; historia nie jest
nadpisywana.

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `patient_id` | UUID | nie | FK -> `patient_profiles.id`, CASCADE |
| `title` | VARCHAR(200) | nie | |
| `description` | VARCHAR(10000) | tak | |
| `body_location` | VARCHAR(120) | tak | |
| `severity` | SMALLINT | tak | 1–10 |
| `started_at`, `ended_at` | TIMESTAMPTZ | tak | `ended_at >= started_at` |
| `observed_at` | TIMESTAMPTZ | nie | czas obserwacji |
| `status` | VARCHAR(32) | nie | `ACTIVE`, `RESOLVED`, `MONITORING` |
| `created_at`, `updated_at` | TIMESTAMPTZ | nie | |

Indeksy: `(patient_id, observed_at DESC)`, `(patient_id, status)`.

### 2.9. `medications`

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `patient_id` | UUID | nie | FK -> `patient_profiles.id`, CASCADE |
| `name` | VARCHAR(200) | nie | |
| `active_ingredient` | VARCHAR(200) | tak | |
| `strength` | VARCHAR(50) | tak | np. `10 mg` |
| `dosage_form` | VARCHAR(80) | tak | np. `tabletka` |
| `prescribed_dose` | VARCHAR(100) | tak | |
| `route` | VARCHAR(80) | tak | |
| `frequency` | VARCHAR(120) | tak | |
| `instructions` | VARCHAR(10000) | tak | |
| `start_date`, `end_date` | DATE | tak | `end_date >= start_date` |
| `prescribing_doctor_id` | UUID | tak | FK -> `doctors.id`, SET NULL |
| `status` | VARCHAR(32) | nie | `ACTIVE`, `PAUSED`, `STOPPED`, `COMPLETED` |
| `notes` | VARCHAR(10000) | tak | |
| `created_at`, `updated_at` | TIMESTAMPTZ | nie | |

Indeksy: `(patient_id, status)`, `prescribing_doctor_id`.
Dawkowanie jest edytowalne wyłącznie przez API aplikacji; AI nie ma endpointu
modyfikującego leki.

### 2.10. `medication_intake_logs`

Historia przyjęć leków (append-only).

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `medication_id` | UUID | nie | FK -> `medications.id`, CASCADE |
| `scheduled_at` | TIMESTAMPTZ | tak | planowany czas |
| `taken_at` | TIMESTAMPTZ | tak | faktyczny czas |
| `amount` | NUMERIC(10,3) | tak | > 0 |
| `unit` | VARCHAR(40) | tak | |
| `status` | VARCHAR(16) | nie | `TAKEN`, `SKIPPED`, `UNKNOWN` |
| `notes` | VARCHAR(10000) | tak | |
| `created_at` | TIMESTAMPTZ | nie | |

Indeksy: `(medication_id, scheduled_at DESC)`, `(medication_id, taken_at DESC)`.

### 2.11. `appointments`

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `patient_id` | UUID | nie | FK -> `patient_profiles.id`, CASCADE |
| `doctor_id` | UUID | tak | FK -> `doctors.id`, SET NULL |
| `scheduled_at` | TIMESTAMPTZ | nie | |
| `ended_at` | TIMESTAMPTZ | tak | `ended_at >= scheduled_at` |
| `location` | VARCHAR(300) | tak | |
| `reason` | VARCHAR(10000) | nie | |
| `status` | VARCHAR(32) | nie | `SCHEDULED`, `COMPLETED`, `CANCELLED`, `MISSED` |
| `notes` | VARCHAR(10000) | tak | |
| `recommendations` | VARCHAR(10000) | tak | |
| `created_at`, `updated_at` | TIMESTAMPTZ | nie | |

Indeksy: `(patient_id, scheduled_at DESC)`, `doctor_id`.

### 2.12. `measurements`

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `patient_id` | UUID | nie | FK -> `patient_profiles.id`, CASCADE |
| `type` | VARCHAR(40) | nie | `BLOOD_PRESSURE`, `HEART_RATE`, `WEIGHT`, `TEMPERATURE`, `GLUCOSE`, `OXYGEN_SATURATION`, `OTHER` |
| `value` | NUMERIC(12,3) | nie | >= 0 |
| `secondary_value` | NUMERIC(12,3) | tak | np. rozkurczowe |
| `unit` | VARCHAR(30) | nie | np. `mmHg` |
| `measured_at` | TIMESTAMPTZ | nie | |
| `source` | VARCHAR(40) | nie | `MANUAL`, `DEVICE`, `IMPORTED`, `UNKNOWN` |
| `notes` | VARCHAR(10000) | tak | |
| `created_at` | TIMESTAMPTZ | nie | |

Wartość i jednostka są osobnymi kolumnami; aplikacja nie konwertuje jednostek
automatycznie. Indeksy: `(patient_id, type, measured_at DESC)`,
`(patient_id, measured_at DESC)`.

### 2.13. `medical_images`

Jedno zdjęcie = jeden rekord.

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK, trwały identyfikator |
| `patient_id` | UUID | nie | FK -> `patient_profiles.id`, CASCADE |
| `symptom_id` | UUID | tak | FK -> `symptoms.id`, SET NULL |
| `document_id` | UUID | tak | FK -> `medical_documents.id`, SET NULL |
| `image_number` | BIGINT | nie | numer w obrębie pacjenta, > 0 |
| `title` | VARCHAR(200) | tak | |
| `description` | VARCHAR(10000) | tak | |
| `category` | VARCHAR(40) | nie | `SKIN`, `INJURY`, `MEDICATION_PACKAGE`, `DOCUMENT`, `OTHER` |
| `original_filename` | VARCHAR(255) | nie | metadana |
| `storage_key` | VARCHAR(500) | nie | unikalny klucz serwera |
| `mime_type` | VARCHAR(150) | nie | |
| `file_size_bytes` | BIGINT | nie | > 0 |
| `width_px`, `height_px` | INTEGER | tak | zarezerwowane; ETAP 1 nie odczytuje wymiarów |
| `sha256_checksum` | VARCHAR(64) | nie | hex |
| `captured_at` | TIMESTAMPTZ | tak | czas wykonania, jeśli znany |
| `uploaded_at` | TIMESTAMPTZ | nie | czas zapisu na serwerze |
| `sequence_number` | INTEGER | tak | opcjonalna kolejność w obrębie serii |
| `ai_processing_status` | VARCHAR(32) | nie | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `NOT_REQUIRED` |
| `created_at`, `updated_at` | TIMESTAMPTZ | nie | |

Ograniczenia: `uk_images_storage_key`, `uk_images_patient_number`
(`UNIQUE(patient_id, image_number)`), `ck_images_number_positive`,
`ck_images_size`, `ck_images_checksum`, `ck_images_category`,
`ck_images_ai_status`, `ck_images_dimensions`.

Indeksy: `(patient_id, uploaded_at DESC)`, `(patient_id, image_number)`,
`symptom_id`, `document_id`.

`captured_at` nie jest traktowane jako zweryfikowane, nawet jeśli pochodzi z EXIF.
`uploaded_at` zawsze oznacza czas zapisu pliku na serwerze.

### 2.14. `image_ai_analyses`

Wyniki analiz zdjęć. Oryginalne zdjęcie pozostaje nietknięte.

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `image_id` | UUID | nie | FK -> `medical_images.id`, CASCADE |
| `model_name` | VARCHAR(150) | nie | np. `mock-ai-provider` |
| `model_version` | VARCHAR(100) | tak | |
| `prompt_version` | VARCHAR(50) | nie | np. `mock-image-v1` |
| `analysis_text` | VARCHAR(50000) | nie | |
| `extracted_metadata` | JSONB | tak | zarezerwowane |
| `created_at` | TIMESTAMPTZ | nie | |

Indeks: `(image_id, created_at DESC)`. W ETAPIE 1 rekordy są jawnie oznaczone
jako mock w treści i w odpowiedzi API (`mock: true`).

### 2.15. `medical_document_extractions`

Zarezerwowane dla OCR/ekstrakcji (ETAP 5).

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `document_id` | UUID | nie | FK -> `medical_documents.id`, CASCADE |
| `extracted_text` | VARCHAR(50000) | tak | |
| `extraction_method` | VARCHAR(80) | nie | |
| `model_name` | VARCHAR(150) | tak | |
| `processing_status` | VARCHAR(32) | nie | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `created_at`, `updated_at` | TIMESTAMPTZ | nie | |

Indeks: `(document_id, created_at DESC)`.

### 2.16. `chat_conversations`

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `patient_id` | UUID | nie | FK -> `patient_profiles.id`, CASCADE |
| `title` | VARCHAR(200) | nie | domyślnie `Nowa rozmowa`, potem początek pytania |
| `created_at`, `updated_at` | TIMESTAMPTZ | nie | |

Indeks: `(patient_id, updated_at DESC)`.

### 2.17. `chat_messages`

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `conversation_id` | UUID | nie | FK -> `chat_conversations.id`, CASCADE |
| `role` | VARCHAR(16) | nie | `USER`, `ASSISTANT`, `SYSTEM` |
| `content` | VARCHAR(20000) | nie | treść |
| `model_name` | VARCHAR(150) | tak | np. `mock-ai-provider` |
| `created_at` | TIMESTAMPTZ | nie | |

Indeks: `(conversation_id, created_at)`.

### 2.18. `ai_message_sources`

Powiązanie odpowiedzi AI z użytymi źródłami.

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `message_id` | UUID | nie | FK -> `chat_messages.id`, CASCADE |
| `source_type` | VARCHAR(32) | nie | `IMAGE`, `DOCUMENT`, `SYMPTOM`, `MEDICATION`, `APPOINTMENT`, `MEASUREMENT`, `CONDITION` |
| `source_id` | UUID | nie | ID rekordu w tabeli wskazanej przez `source_type` |
| `relevance_description` | VARCHAR(10000) | tak | etykieta i powód użycia |
| `created_at` | TIMESTAMPTZ | nie | |

Indeksy: `message_id`, `(source_type, source_id)`.

`source_id` jest polimorficzne i **nie ma** FK do wielu tabel. Integralność
właściciela jest sprawdzana w warstwie aplikacji (`AiSourceValidator`) przed
zapisem. Wybór jest świadomy: jeden FK nie może wskazywać wielu tabel, a osobne
tabele źródłowe komplikowałyby model i zapytania.

### 2.19. `ai_processing_jobs`

Zadania analizy (przyszłe, asynchroniczne).

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `patient_id` | UUID | nie | FK -> `patient_profiles.id`, CASCADE |
| `resource_type` | VARCHAR(40) | nie | `IMAGE`, `DOCUMENT` |
| `resource_id` | UUID | nie | ID zasobu |
| `status` | VARCHAR(32) | nie | `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED` |
| `model_name` | VARCHAR(150) | tak | |
| `error_message` | VARCHAR(10000) | tak | bez sekretów i zbędnych danych medycznych |
| `created_at`, `started_at`, `completed_at` | TIMESTAMPTZ | tak/nie | |

Indeksy: `(patient_id, status)`, `(resource_type, resource_id)`.

### 2.20. `refresh_tokens`

| Kolumna | Typ | Null | Opis |
|---|---|---|---|
| `id` | UUID | nie | PK |
| `user_id` | UUID | nie | FK -> `users.id`, CASCADE |
| `token_hash` | VARCHAR(64) | nie | SHA-256 tokenu, unikalny |
| `expires_at` | TIMESTAMPTZ | nie | |
| `revoked_at` | TIMESTAMPTZ | tak | |
| `created_at` | TIMESTAMPTZ | nie | |

Ograniczenia: `uk_refresh_tokens_hash`, `ck_refresh_tokens_hash` (hex).
Indeksy: `user_id`, `expires_at`.

## 3. Relacje

```
users 1---1 patient_profiles
patient_profiles 1---1 patient_image_counters
patient_profiles 1---* doctors
patient_profiles 1---* medical_documents
patient_profiles 1---* medical_conditions
patient_profiles 1---* allergies
patient_profiles 1---* symptoms
patient_profiles 1---* medications
patient_profiles 1---* appointments
patient_profiles 1---* measurements
patient_profiles 1---* medical_images
patient_profiles 1---* chat_conversations
patient_profiles 1---* ai_processing_jobs
doctors 1---* medications (prescribing_doctor_id, SET NULL)
doctors 1---* appointments (SET NULL)
medical_documents 1---* medical_conditions (source_document_id, SET NULL)
medical_documents 1---* medical_images (document_id, SET NULL)
medical_documents 1---* medical_document_extractions
symptoms 1---* medical_images (symptom_id, SET NULL)
medications 1---* medication_intake_logs
medical_images 1---* image_ai_analyses
chat_conversations 1---* chat_messages
chat_messages 1---* ai_message_sources
users 1---* refresh_tokens
```

## 4. Zasady numeracji zdjęć

1. Przy rejestracji tworzony jest wiersz `patient_image_counters` z `0`.
2. Upload w transakcji wykonuje `SELECT ... FOR UPDATE` na wierszu pacjenta.
3. `last_image_number` jest zwiększany o 1 i zapisywany jako `image_number`.
4. `UNIQUE(patient_id, image_number)` chroni przed duplikatem w razie błędu.
5. Usunięcie zdjęcia nie zmienia licznika. Numer nie jest ponownie używany.
6. Numeracja nie używa `COUNT(*) + 1`.

Test integracyjny sprawdza: dodanie zdjęć 1, 2, 3, usunięcie 2, dodanie
kolejnego -> numer 4.

## 5. Powiązanie zdjęcia z objawem

- `medical_images.symptom_id` -> `symptoms.id`, nullable, `ON DELETE SET NULL`.
- Jeden objaw może mieć wiele zdjęć.
- Zdjęcie może nie mieć objawu.
- Przy uploadzie i edycji backend sprawdza, że objaw należy do tego samego
  pacjenta; w przeciwnym razie zwraca 400.
- `GET /api/v1/app/images?symptomId=<uuid>` filtruje zdjęcia objawu.

## 6. Wiadomości AI i źródła

- `ai_message_sources.message_id` wskazuje wiadomość asystenta.
- `source_type` + `source_id` wskazują rekord w jednej z tabel:
  `medical_images`, `medical_documents`, `symptoms`, `medications`,
  `appointments`, `measurements`, `medical_conditions`.
- Przed zapisem `AiSourceValidator` sprawdza istnienie rekordu i `patient_id`.
- API zwraca źródła razem z wiadomością, dzięki czemu aplikacja może pokazać
  „na jakiej podstawie” powstała odpowiedź.
- W ETAPIE 1 źródła pochodzą z kontekstu zbudowanego przez backend; model ich
  nie wybiera samodzielnie.

## 7. Przykładowe rekordy fikcyjnego pacjenta

Poniższe dane są **fikcyjne** i nie są seedowane do bazy.

`users`:

| email | display_name |
|---|---|
| `jan.testowy@example.test` | `Jan Testowy` |

`patient_profiles`:

| first_name | last_name | date_of_birth | sex | height_cm | weight_kg | blood_type |
|---|---|---|---|---|---|---|
| Jan | Testowy | 1985-04-12 | MALE | 180.5 | 82.0 | A_POSITIVE |

`allergies`:

| substance | reaction | severity | status |
|---|---|---|---|
| Penicylina | Wysypka | MODERATE | ACTIVE |

`medications`:

| name | strength | dosage_form | prescribed_dose | frequency | status |
|---|---|---|---|---|---|
| Metformina | 500 mg | tabletka | 1 tabletka | 2 razy dziennie | ACTIVE |

`medication_intake_logs`:

| scheduled_at | taken_at | amount | unit | status |
|---|---|---|---|---|
| 2026-01-10T08:00:00Z | 2026-01-10T08:05:00Z | 1.000 | tabletka | TAKEN |

`symptoms`:

| title | body_location | severity | observed_at | status |
|---|---|---|---|---|
| Ból głowy | głowa | 6 | 2026-01-09T18:30:00Z | ACTIVE |

`appointments`:

| scheduled_at | reason | status |
|---|---|---|
| 2026-02-01T09:00:00Z | Kontrola | SCHEDULED |

`measurements`:

| type | value | secondary_value | unit | measured_at | source |
|---|---|---|---|---|---|
| BLOOD_PRESSURE | 130.000 | 85.000 | mmHg | 2026-01-10T07:30:00Z | MANUAL |

`medical_images` (fragment):

| image_number | category | title | symptom_id | uploaded_at |
|---|---|---|---|---|
| 1 | SKIN | Zmiana na skórze | (objaw Ból głowy – tylko przykład) | 2026-01-10T10:00:00Z |
| 2 | SKIN | Kontrola zmiany | (ten sam objaw) | 2026-01-17T10:00:00Z |
| 3 | SKIN | Kolejna kontrola | (ten sam objaw) | 2026-02-01T10:00:00Z |

`chat_conversations` + `chat_messages`:

| conversation.title | role | content |
|---|---|---|
| Jakie przyjmuję leki? | USER | Jakie przyjmuję leki? |
| Jakie przyjmuję leki? | ASSISTANT | MOCK — ... (odpowiedź demonstracyjna) |

`ai_message_sources`:

| source_type | source_id | relevance_description |
|---|---|---|
| MEDICATION | UUID Metforminy | Lek: Metformina 500 mg — lek pobrany precyzyjnym zapytaniem |
| SYMPTOM | UUID Bólu głowy | Objaw: Ból głowy — objaw pacjenta użyty w kontekście |

## 8. Lokalny magazyn plików

`STORAGE_ROOT` (domyślnie `../storage` względem katalogu backendu) nie jest
udostępniany jako katalog statyczny Springa.

Struktura:

```
storage/
  patients/<patient-uuid>/
    images/<random-uuid>.jpg|png|webp
    documents/<random-uuid>.pdf|jpg|png|webp
```

Zasady:

- `storage_key` jest generowany przez serwer i zapisywany w DB.
- Oryginalna nazwa pliku służy tylko do metadanych i `Content-Disposition`.
- Ścieżka wynikowa jest normalizowana i musi pozostać wewnątrz `STORAGE_ROOT`
  (ochrona przed `../`).
- Pliki są serwowane przez kontrolery `GET .../{id}/file` po sprawdzeniu
  właściciela, z `Cache-Control: no-store` i `X-Content-Type-Options: nosniff`.
- Dozwolone obrazy: JPEG, PNG, WebP; dokumenty: PDF, JPEG, PNG, WebP.
- Maksymalny rozmiar: 25 MB (konfigurowalne).
- Nazwa pliku na dysku nigdy nie pochodzi od użytkownika.
- Kopie zapasowe powinny obejmować jednocześnie PostgreSQL i `storage/`.
