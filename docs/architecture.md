# Architektura

## 1. Cel i zakres ETAPU 1

Backend prywatnej karty pacjenta działający lokalnie na PC:

- jeden modularny monolit Spring Boot,
- PostgreSQL jako źródło prawdy,
- lokalny dysk na pliki,
- dwa obszary API: aplikacyjny (`/api/v1/app`) i AI (`/api/v1/ai`),
- wspólne uwierzytelnianie i autoryzacja,
- AI na tym etapie wyłącznie jako szkielet z `MockAiProvider`.

Aplikacja Android, LM Studio i analiza obrazów nie są częścią tego etapu.

## 2. Kontekst systemu

```
Telefon Android (ETAP 2)
        |
        | HTTPS/HTTP w prywatnej sieci Wi-Fi, Bearer JWT
        v
+---------------------------------------------------+
| Backend Spring Boot (PC)                           |
|                                                   |
|  /api/v1/app          /api/v1/ai                  |
|  auth, karta, leki,   rozmowy, kontekst pacjenta,  |
|  wizyty, pliki...     źródła, MockAiProvider       |
|          \                 /                       |
|           \               /                        |
|            +-- PostgreSQL +                        |
|            +-- lokalny magazyn plików              |
+---------------------------------------------------+
                          |
                          | (ETAP 4) OpenAI-compatible API
                          v
                 LM Studio na PC (później)
```

Zasady:

- Android nie łączy się bezpośrednio z PostgreSQL, dyskiem ani LM Studio.
- Model AI nigdy nie dostaje ścieżek systemowych. Backend weryfikuje właściciela
  zasobu i sam odczytuje plik, jeśli zajdzie potrzeba.
- PostgreSQL jest wystawiony tylko na `127.0.0.1` (Docker Compose).
- W ETAPIE 1 nie ma publicznego wystawienia usług.

## 3. Modularny monolit

```
com.patientcard
├── auth          logowanie, rejestracja, refresh tokeny
├── patient       karta pacjenta, bieżący pacjent
├── allergy
├── condition
├── medication    leki + historia przyjęć
├── doctor
├── appointment
├── symptom
├── measurement
├── document      metadane + upload/pobieranie
├── image         metadane, numeracja, upload/pobieranie
├── storage       walidacja plików, bezpieczny zapis/odczyt
├── ai
│   ├── provider  AiProvider + MockAiProvider
│   ├── context   PatientContextProvider, walidacja źródeł
│   └── ...       chat, rozmowy, analiza obrazu (mock)
├── security      JWT, filtr, principal
├── config        properties, Security, OpenAPI
├── common        błędy, paginacja, encje bazowe
├── entity        encje JPA
└── repository    repozytoria Spring Data
```

Każdy moduł ma własny controller, service, DTO i (czasem współdzielone)
repository. Kontrolery nie zawierają logiki biznesowej.

## 4. Warstwy i przepływ żądania

```
HTTP -> JwtAuthenticationFilter -> Controller (walidacja DTO)
     -> Service (@Transactional, ownership, reguły biznesowe)
     -> Repository (JPA)
     -> PostgreSQL
```

- `UserPrincipal` (UUID użytkownika, email) jest tworzony wyłącznie z podpisanego JWT.
- `CurrentPatientService` mapuje `user_id` -> `patient_profile.id`.
- Każda operacja na zasobie potomnym używa `findByIdAndPatientId(...)`.
- Brak zaufania do `patientId`/`userId` z body — takich pól nie ma w DTO.
- Błędy są mapowane centralnie na `ProblemDetail` (RFC 9457).

## 5. Kluczowe decyzje

| Decyzja | Uzasadnienie |
|---|---|
| Osobny build `backend/` | nie miesza Spring Boot z buildem Android Studio |
| UUID w aplikacji (`GenerationType.UUID`) | identyfikatory trwałe, bezpieczne w API |
| Klucze obce jako skalarne `UUID` w encjach | prostsze zapytania i brak problemów lazy-loading; ownership jawny |
| `VARCHAR` + `CHECK` zamiast natywnych enumów PostgreSQL | łatwiejsza ewolucja schematu i mapowanie JPA |
| Flyway + `ddl-auto: validate` | schemat wyłącznie z migracji, walidacja encji przy starcie |
| Opaque refresh token + hash w DB | możliwa rotacja i revokacja, brak tokenów w plaintext |
| Licznik `patient_image_counters` + `SELECT ... FOR UPDATE` | bezpieczna numeracja zdjęć bez `COUNT(*) + 1` |
| Metadane w DB, pliki na dysku | brak BLOB w PostgreSQL, kontrolowany dostęp |
| `storage_key` generowany serwerowo | brak path traversal, brak zaufania do nazwy pliku |
| `ai_message_sources` polimorficzne | elastyczne źródła odpowiedzi; ownership walidowany w aplikacji |
| Mock provider za interfejsem | wymiana na LM Studio bez zmian w logice biznesowej |
| `origin` w `medical_conditions` | rozróżnienie wpisu pacjenta od rozpoznania lekarza |

## 6. Spójność plików i bazy

Zapis:

1. walidacja rozmiaru, rozszerzenia, MIME i sygnatury,
2. zapis pliku pod losowym `storage_key`,
3. rejestracja sprzątania pliku przy rollbacku transakcji,
4. zapis metadanych w PostgreSQL,
5. commit.

Jeśli transakcja się nie powiedzie, plik jest usuwany w `afterCompletion`.

Usuwanie:

1. usunięcie rekordu w transakcji,
2. po commicie usunięcie pliku (`deleteAfterCommit`).

Jeśli usunięcie pliku po commicie się nie uda, pozostaje osierocony plik i
ostrzeżenie w logu. Automatyczny sweeper nie został dodany w ETAPIE 1.

## 7. Numeracja zdjęć

- Każdy pacjent ma wiersz w `patient_image_counters`.
- W transakcji uploadu wykonywany jest `SELECT ... FOR UPDATE` na tym wierszu.
- `last_image_number` rośnie o 1, wartość trafia do `medical_images.image_number`.
- Unikalność `(patient_id, image_number)` jest dodatkowym zabezpieczeniem.
- Usunięcie zdjęcia nie zmniejsza licznika — numery nie są ponownie używane.

## 8. AI a dane pacjenta

- `PatientContextProvider` buduje mały kontekst na podstawie pytania (routing
  słów kluczowych + precyzyjne zapytania do bazy).
- Kontekst ma limity liczby elementów i znaków.
- Źródła są zapisywane w `ai_message_sources`; przed zapisem sprawdzany jest
  właściciel każdego źródła.
- Mock nie udaje rzeczywistej analizy — treść i flaga `mock` są jednoznaczne.
- Docelowo LM Studio będzie inną implementacją `AiProvider`.

## 9. Gotowość na wielu użytkowników

Schemat od początku ma `patient_id`/`user_id` i więzy własności. Autoryzacja jest
oparta na principals, nie na parametrach żądania. Nie ma jeszcze ról ani
współdzielenia karty między użytkownikami — to możliwe w przyszłości bez zmiany
fundamentów.

## 10. Ograniczenia architektury ETAPU 1

- brak asynchronicznych zadań AI (tabela `ai_processing_jobs` istnieje, ale
  analiza mock jest synchroniczna),
- brak OCR, ekstrakcji dokumentów i analizy obrazu,
- brak cache'u i wyszukiwania semantycznego,
- brak rate limitingu,
- brak automatycznego audytu dostępu.
