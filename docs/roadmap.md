# Roadmapa

## ETAP 1 — backend i fundament danych (ukończony)

- modularny monolit Spring Boot 3.5 / Kotlin,
- PostgreSQL 16 + Flyway (20 tabel, więzy, indeksy),
- rejestracja/logowanie, JWT + rotowane refresh tokeny,
- karta pacjenta, alergie, schorzenia, leki, historia przyjęć, lekarze,
  wizyty, objawy, pomiary,
- dokumenty i zdjęcia: upload, walidacja, pobieranie, usuwanie, numeracja zdjęć,
- API AI: rozmowy, historia, źródła, analiza zdjęcia jako jawny mock,
- OpenAPI/Swagger, Actuator health, dokumentacja, testy integracyjne.

**Status:** zakończony i zgłoszony do akceptacji. ETAP 2 nie został rozpoczęty.

## ETAP 2 — aplikacja Android (następny, po akceptacji)

Cel: minimalny, użyteczny klient prywatnej karty pacjenta.

- nowy moduł aplikacji Compose (lub wydzielenie obecnego szablonu),
- ekran logowania i rejestracji,
- bezpieczne przechowywanie tokenów (EncryptedSharedPreferences / DataStore),
- klient HTTP (Retrofit + kotlinx.serialization lub Ktor),
- automatyczne odświeżanie access tokenu i wylogowanie przy `401`,
- profil pacjenta: podgląd i edycja,
- listy: alergie, schorzenia, leki, objawy, pomiary, wizyty, lekarze,
- dodawanie/edycja/usuwanie podstawowych rekordów,
- upload zdjęcia i dokumentu z telefonu, podgląd/pobranie,
- obsługa błędów sieci i komunikaty po polsku,
- prosta nawigacja i stany ładowania/pustki,
- konfiguracja adresu backendu w prywatnej sieci Wi-Fi (bez hardkodowania IP),
- testy jednostkowe logiki i podstawowe testy UI.

Poza zakresem ETAPU 2: rzeczywiste AI, OCR, powiadomienia, tryb offline.

## ETAP 3 — dopracowanie doświadczenia pacjenta

Kandydaci (do potwierdzenia):

- dashboard/chronologia zdarzeń,
- wyszukiwanie i filtry,
- wykresy pomiarów,
- lepsze zarządzanie zdjęciami i seriami,
- eksport danych do PDF/CSV,
- motyw, dostępność, tryb ciemny,
- testy instrumentowane i CI.

## ETAP 4 — lokalny model AI (LM Studio)

- `LmStudioAiProvider` implementujący `AiProvider`,
- OpenAI-compatible API, konfigurowalny URL/model, timeouty,
- streaming odpowiedzi (opcjonalnie),
- lepszy kontekst pacjenta i cytowanie źródeł w UI,
- obsługa błędów i limitów bez blokowania aplikacji,
- nadal wyraźne oznaczenie: AI nie zastępuje lekarza.

## ETAP 5 — dokumenty i analiza obrazu

- OCR/ekstrakcja tekstu z PDF i zdjęć,
- zapis w `medical_document_extractions`,
- analiza zdjęć przez model vision,
- wersjonowanie promptów i modeli,
- kolejka zadań (`ai_processing_jobs`) i statusy w UI,
- zgoda użytkownika i jasne ostrzeżenia.

## ETAP 6 — utwardzenie i prywatność

- HTTPS/reverse proxy, rate limiting, blokada logowania,
- szyfrowanie plików, rotacja sekretów,
- audyt dostępu, retencja i eksport danych,
- kopie zapasowe i odtwarzanie,
- testy bezpieczeństwa i wydajności,
- dokumentacja wdrożenia.

## Zasady realizacji

- Każdy etap kończy się raportem, testami i zatrzymaniem przed następnym.
- Najpierw działający przepływ end-to-end, potem funkcje dodatkowe.
- Żadnych prawdziwych danych medycznych w repozytorium, testach i seedach.
- Mock AI nigdy nie jest prezentowany jako rzeczywista analiza.
- Zmiany schematu wyłącznie przez nowe migracje Flyway.
