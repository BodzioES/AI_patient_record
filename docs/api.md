# API

Base URL: `http://localhost:8080/api/v1`

Dokumentacja interaktywna: `http://localhost:8080/swagger-ui.html`
(specyfikacja: `http://localhost:8080/v3/api-docs`).

## 1. Zasady wspólne

- Format: JSON (`application/json`), pliki: `multipart/form-data`.
- Identyfikatory: UUID w formacie tekstowym.
- Daty i znaczniki czasu: ISO-8601 UTC, np. `2026-01-10T08:05:00Z`.
- Daty kalendarzowe: `YYYY-MM-DD`.
- Wszystkie żądania poza rejestracją, logowaniem, refresh, logout, Swaggerem
  i `/actuator/health` wymagają nagłówka:

```http
Authorization: Bearer <accessToken>
```

- Właściciel danych wynika **wyłącznie z tokenu**. DTO nie zawierają `userId`
  ani `patientId`; próba użycia cudzego zasobu kończy się `404`.
- Access token żyje 15 minut. Po wygaśnięciu należy wykonać `/auth/refresh`.
- Refresh token jest jednorazowy: każde odświeżenie unieważnia poprzedni.
- `logout` unieważnia refresh token; access token wygasa naturalnie.

### 1.1. Paginacja

Endpointy listujące zwracają:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Parametry: `page` (od 0), `size` (domyślnie 20, maks. 100), `sort`
(np. `sort=createdAt,desc`). Domyślne sortowanie jest opisane przy endpointach.

### 1.2. Błędy

Błędy mają format RFC 9457 (`application/problem+json`) i pole `code`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Dane wejściowe są nieprawidłowe.",
  "code": "VALIDATION_ERROR",
  "errors": {
    "email": "Podaj poprawny adres email.",
    "password": "Hasło musi mieć od 8 do 72 znaków."
  }
}
```

| HTTP | `code` | Znaczenie |
|---|---|---|
| 400 | `VALIDATION_ERROR` | błędy walidacji DTO (z polem `errors`) |
| 400 | `INVALID_JSON` | nieczytelny JSON |
| 400 | `MISSING_PART` | brak wymaganego parametru lub pliku |
| 400 | `BAD_REQUEST` | reguła biznesowa, np. powiązanie z cudzym zasobem |
| 401 | `UNAUTHORIZED` | brak/nieprawidłowy token lub dane logowania |
| 403 | `FORBIDDEN` | brak uprawnień |
| 404 | `NOT_FOUND` | brak zasobu lub zasób innego pacjenta |
| 409 | `CONFLICT` | duplikat (np. email) |
| 409 | `DATA_CONFLICT` | naruszenie więzów integralności danych |
| 413 | `FILE_TOO_LARGE` | plik przekracza limit |
| 415 | `INVALID_FILE` | niedozwolony typ/rozszerzenie/sygnatura pliku |
| 500 | `INTERNAL_ERROR` | nieoczekiwany błąd (bez stack trace) |
| 503 | `AI_PROVIDER_ERROR` | błąd dostawcy AI |

## 2. Uwierzytelnianie — `/api/v1/app/auth`

### `POST /register` — 201

```json
{
  "email": "pacjent@example.test",
  "password": "Minimum8Znakow!",
  "displayName": "Anna Testowa"
}
```

Tworzy użytkownika, kartę pacjenta i licznik zdjęć. Zwraca ten sam kształt co
`/login`. Email jest normalizowany do lowercase. Hasło: 8–72 znaki.
Duplikat emaila → `409 CONFLICT`.

### `POST /login` — 200

```json
{ "email": "pacjent@example.test", "password": "Minimum8Znakow!" }
```

Błędne dane → `401 UNAUTHORIZED` (komunikat nie zdradza, czy email istnieje).

### Odpowiedź `AuthResponse`

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "refreshToken": "<opaque>",
  "user": {
    "id": "7e5f0f0e-...",
    "email": "pacjent@example.test",
    "displayName": "Anna Testowa",
    "enabled": true,
    "createdAt": "2026-01-10T08:00:00Z"
  },
  "patientId": "8b3f6f2a-..."
}
```

### `POST /refresh` — 200

```json
{ "refreshToken": "<opaque>" }
```

Rotacja: stary token zostaje unieważniony, przychodzi nowy `AuthResponse`.
Użycie unieważnionego/wygasłego tokenu → `401`.

### `POST /logout` — 204

```json
{ "refreshToken": "<opaque>" }
```

Idempotentne; nieznany token również daje `204`. Access token pozostaje ważny do
swojego wygaśnięcia.

### `GET /me` — 200

```json
{
  "id": "7e5f0f0e-...",
  "email": "pacjent@example.test",
  "displayName": "Anna Testowa",
  "enabled": true,
  "createdAt": "2026-01-10T08:00:00Z",
  "patientId": "8b3f6f2a-..."
}
```

## 3. Karta pacjenta — `/api/v1/app/patient`

| Metoda | Ścieżka | Opis |
|---|---|---|
| GET | `/patient` | karta bieżącego użytkownika |
| PUT | `/patient` | aktualizacja karty |

`PUT` zastępuje wartości: pole pominięte lub `null` czyści daną (poza walidacją).
Pola: `firstName`, `lastName`, `dateOfBirth`, `sex`, `heightCm`, `weightKg`,
`bloodType`, `notes`.

Wartości `sex`: `FEMALE`, `MALE`, `OTHER`, `UNKNOWN`.
Wartości `bloodType`: `A_POSITIVE`, `A_NEGATIVE`, `B_POSITIVE`, `B_NEGATIVE`,
`AB_POSITIVE`, `AB_NEGATIVE`, `ZERO_POSITIVE`, `ZERO_NEGATIVE`, `UNKNOWN`.

## 4. Zasoby kliniczne

Wzorzec dla wszystkich poniższych:

| Metoda | Ścieżka | Status | Opis |
|---|---|---|---|
| GET | `/{resource}` | 200 | paginowana lista |
| GET | `/{resource}/{id}` | 200 | szczegóły |
| POST | `/{resource}` | 201 | utworzenie |
| PUT | `/{resource}/{id}` | 200 | aktualizacja |
| DELETE | `/{resource}/{id}` | 204 | usunięcie |

Zasoby: `allergies`, `conditions`, `medications`, `doctors`, `appointments`,
`symptoms`, `measurements`, `medication-intake`.

Filtry list:

| Zasób | Filtr | Domyślne sortowanie |
|---|---|---|
| `allergies` | `status` | `createdAt,desc` |
| `conditions` | `status` | `createdAt,desc` |
| `medications` | `status` | `createdAt,desc` |
| `doctors` | – | `lastName,asc` |
| `appointments` | `status`, `from`, `to` (ISO-8601) | `scheduledAt,desc` |
| `symptoms` | `status` | `observedAt,desc` |
| `measurements` | `type` | `measuredAt,desc` |
| `medication-intake` | **wymagany** `medicationId` | `scheduledAt,desc` |

### 4.1. Alergie

```json
{
  "substance": "Penicylina",
  "reaction": "Wysypka",
  "severity": "MODERATE",
  "status": "ACTIVE",
  "diagnosedAt": "2020-06-01",
  "notes": "Fikcyjny wpis"
}
```

`severity`: `MILD`, `MODERATE`, `SEVERE`, `LIFE_THREATENING`, `UNKNOWN`.
`status`: `ACTIVE`, `RESOLVED`, `SUSPECTED`.

### 4.2. Schorzenia

```json
{
  "name": "Cukrzyca typu 2",
  "description": "Rozpoznanie fikcyjne",
  "status": "ACTIVE",
  "onsetDate": "2019-02-01",
  "resolvedDate": null,
  "sourceDocumentId": null,
  "origin": "CLINICIAN_DOCUMENTED"
}
```

`status`: `ACTIVE`, `IN_REMISSION`, `RESOLVED`, `SUSPECTED`.
`origin`: `PATIENT_REPORTED`, `CLINICIAN_DOCUMENTED`, `IMPORTED`.
`sourceDocumentId` musi należeć do tego samego pacjenta.

### 4.3. Leki

```json
{
  "name": "Metformina",
  "activeIngredient": "metformini hydrochloridum",
  "strength": "500 mg",
  "dosageForm": "tabletka",
  "prescribedDose": "1 tabletka",
  "route": "doustnie",
  "frequency": "2 razy dziennie",
  "instructions": "Przyjmować po posiłku",
  "startDate": "2026-01-01",
  "endDate": null,
  "prescribingDoctorId": null,
  "status": "ACTIVE",
  "notes": null
}
```

`status`: `ACTIVE`, `PAUSED`, `STOPPED`, `COMPLETED`.
`endDate` nie może być wcześniejsza niż `startDate`.
`prescribingDoctorId` musi należeć do tego samego pacjenta.

### 4.4. Historia przyjęć leku

```json
{
  "medicationId": "<uuid>",
  "scheduledAt": "2026-01-10T08:00:00Z",
  "takenAt": "2026-01-10T08:05:00Z",
  "amount": 1.0,
  "unit": "tabletka",
  "status": "TAKEN",
  "notes": null
}
```

`status`: `TAKEN`, `SKIPPED`, `UNKNOWN`.

### 4.5. Lekarze

```json
{
  "firstName": "Ewa",
  "lastName": "Lekarska",
  "specialty": "Kardiologia",
  "facilityName": "Przychodnia Testowa",
  "phone": "500-000-000",
  "email": "ewa.lekarska@example.test",
  "address": "ul. Testowa 1",
  "notes": null
}
```

### 4.6. Wizyty

```json
{
  "doctorId": null,
  "scheduledAt": "2026-02-01T09:00:00Z",
  "endedAt": null,
  "location": "Przychodnia Testowa, pokój 3",
  "reason": "Kontrola",
  "status": "SCHEDULED",
  "notes": null,
  "recommendations": null
}
```

`status`: `SCHEDULED`, `COMPLETED`, `CANCELLED`, `MISSED`.
`endedAt` nie może być wcześniejsze niż `scheduledAt`.

### 4.7. Objawy

```json
{
  "title": "Ból głowy",
  "description": "Ucisk w skroniach",
  "bodyLocation": "głowa",
  "severity": 6,
  "startedAt": "2026-01-09T18:00:00Z",
  "endedAt": null,
  "observedAt": "2026-01-09T18:30:00Z",
  "status": "ACTIVE"
}
```

`severity`: 1–10. `status`: `ACTIVE`, `RESOLVED`, `MONITORING`.
Brak `observedAt` oznacza czas zapisu. Kolejna obserwacja tego samego objawu to
nowy rekord, nie nadpisanie historii.

### 4.8. Pomiary

```json
{
  "type": "BLOOD_PRESSURE",
  "value": 130.0,
  "secondaryValue": 85.0,
  "unit": "mmHg",
  "measuredAt": "2026-01-10T07:30:00Z",
  "source": "MANUAL",
  "notes": null
}
```

`type`: `BLOOD_PRESSURE`, `HEART_RATE`, `WEIGHT`, `TEMPERATURE`, `GLUCOSE`,
`OXYGEN_SATURATION`, `OTHER`.
`source`: `MANUAL`, `DEVICE`, `IMPORTED`, `UNKNOWN`.
Dla ciśnienia `value` = skurczowe, `secondaryValue` = rozkurczowe.

## 5. Dokumenty — `/api/v1/app/documents`

| Metoda | Ścieżka | Opis |
|---|---|---|
| GET | `/documents` | lista, `uploadedAt,desc` |
| GET | `/documents/{id}` | metadane |
| POST | `/documents` | upload multipart, 201 |
| PUT | `/documents/{id}` | metadane |
| GET | `/documents/{id}/file` | pobranie pliku (attachment) |
| DELETE | `/documents/{id}` | usunięcie rekordu i pliku, 204 |

Upload — pola formularza:

| Pole | Wymagane | Opis |
|---|---|---|
| `file` | tak | PDF, JPG, PNG, WEBP; maks. 25 MB |
| `documentType` | tak | dowolny tekst, maks. 40 znaków |
| `title` | tak | maks. 200 znaków |
| `description` | nie | maks. 10 000 znaków |
| `documentDate` | nie | `YYYY-MM-DD`, nie z przyszłości |

Walidacja obejmuje rozszerzenie, deklarowany MIME i sygnaturę początku pliku.
`storageKey` nie jest zwracany. Odpowiedź zawiera m.in. `sha256Checksum`,
`fileSizeBytes`, `processingStatus` (`PENDING` w ETAPIE 1).

```bash
curl -X POST http://localhost:8080/api/v1/app/documents \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@wynik.pdf;type=application/pdf" \
  -F "documentType=LAB_RESULT" \
  -F "title=Wynik badania" \
  -F "documentDate=2026-01-08"
```

## 6. Zdjęcia — `/api/v1/app/images`

| Metoda | Ścieżka | Opis |
|---|---|---|
| GET | `/images` | lista, `imageNumber,desc`; filtr `symptomId` |
| GET | `/images/{id}` | metadane |
| POST | `/images` | upload multipart, 201 |
| PUT | `/images/{id}` | metadane |
| GET | `/images/{id}/file` | podgląd pliku (inline) |
| DELETE | `/images/{id}` | usunięcie rekordu i pliku, 204 |

Upload — pola formularza:

| Pole | Wymagane | Opis |
|---|---|---|
| `file` | tak | JPG, PNG, WEBP; maks. 25 MB |
| `title` | nie | maks. 200 znaków |
| `description` | nie | maks. 10 000 znaków |
| `category` | nie | domyślnie `OTHER` |
| `symptomId` | nie | objaw tego samego pacjenta |
| `documentId` | nie | dokument tego samego pacjenta |
| `capturedAt` | nie | ISO-8601 |
| `sequenceNumber` | nie | dodatni; kolejność w serii |

`category`: `SKIN`, `INJURY`, `MEDICATION_PACKAGE`, `DOCUMENT`, `OTHER`.

`imageNumber` jest nadawany serwerowo, per pacjent, w transakcji z blokadą
wiersza licznika. Numery nie są ponownie używane po usunięciu zdjęcia.
Para `(patientId, imageNumber)` jest unikalna.

```bash
curl -X POST http://localhost:8080/api/v1/app/images \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@zdjecie.jpg;type=image/jpeg" \
  -F "title=Zmiana skórna" \
  -F "category=SKIN" \
  -F "symptomId=$SYMPTOM_ID"
```

## 7. AI — `/api/v1/ai`

> ETAP 1 używa wyłącznie `MockAiProvider`. Odpowiedzi zawierają `mock: true`
> oraz jawne oznaczenie „MOCK”. Nie jest to analiza medyczna.

### `POST /chat` — 200

```json
{
  "conversationId": null,
  "message": "Jakie przyjmuję leki i jakie mam objawy?"
}
```

`conversationId` opcjonalne; brak tworzy nową rozmowę. `message`: 1–4000 znaków.

Odpowiedź:

```json
{
  "conversationId": "<uuid>",
  "userMessageId": "<uuid>",
  "assistantMessageId": "<uuid>",
  "answer": "MOCK — to nie jest odpowiedź rzeczywistego modelu AI. ...",
  "modelName": "mock-ai-provider",
  "modelVersion": "1",
  "mock": true,
  "sources": [
    {
      "type": "MEDICATION",
      "sourceId": "<uuid>",
      "relevanceDescription": "Lek: Ramipryl 5 mg — Lek pobrany precyzyjnym zapytaniem po patient_id."
    }
  ],
  "createdAt": "2026-01-12T09:10:00Z"
}
```

Backend buduje kontekst na podstawie słów kluczowych pytania, zapisuje wiadomość
użytkownika i odpowiedź oraz źródła. `sourceId` jest walidowany pod kątem
właściciela przed zapisem.

### `GET /conversations` — 200

Lista rozmów, `updatedAt,desc`, z `messageCount`.

### `GET /conversations/{id}` — 200

Rozmowa z wiadomościami (`USER` / `ASSISTANT` / `SYSTEM`) i źródłami każdej
odpowiedzi. Cudza rozmowa → `404`.

### `DELETE /conversations/{id}` — 204

Usuwa rozmowę i kaskadowo wiadomości oraz źródła.

### `POST /images/{imageId}/analyze` — 200

Tworzy rekord `image_ai_analyses` i wpis zadania AI. Dla mocka nie odczytuje
pikseli. Ustawia `aiProcessingStatus` zdjęcia na `COMPLETED`.

```json
{
  "id": "<uuid>",
  "imageId": "<uuid>",
  "imageNumber": 1,
  "modelName": "mock-ai-provider",
  "modelVersion": "1",
  "promptVersion": "mock-image-v1",
  "analysisText": "MOCK — nie wykonano analizy treści zdjęcia. ...",
  "createdAt": "2026-01-12T09:20:00Z",
  "mock": true,
  "notice": "Wynik pochodzi z MockAiProvider. Nie wykonano rzeczywistej analizy obrazu ani diagnozy medycznej."
}
```

### `GET /images/{imageId}/analysis` — 200

```json
{
  "imageId": "<uuid>",
  "imageNumber": 1,
  "analyses": [],
  "mock": true,
  "notice": "Wynik pochodzi z MockAiProvider. ..."
}
```

## 8. Przykładowy przepływ

```bash
BASE=http://localhost:8080/api/v1

# 1. Rejestracja
curl -s -X POST "$BASE/app/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.test","password":"DemoHaslo123!","displayName":"Demo"}'

# 2. Zapis tokenu (jq)
TOKEN=$(curl -s -X POST "$BASE/app/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.test","password":"DemoHaslo123!"}' | jq -r .accessToken)

# 3. Karta pacjenta
curl -s "$BASE/app/patient" -H "Authorization: Bearer $TOKEN"

# 4. Lek
curl -s -X POST "$BASE/app/medications" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ramipryl","strength":"5 mg","status":"ACTIVE"}'

# 5. Rozmowa z mockiem
curl -s -X POST "$BASE/ai/chat" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"Jakie przyjmuję leki?"}'
```

PowerShell: odpowiedniki `curl` to `Invoke-RestMethod`; nagłówek ustawia się przez
`-Headers @{ Authorization = "Bearer $TOKEN" }`.
