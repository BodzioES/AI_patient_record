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

**Status:** zakończony i zgłoszony do akceptacji. ETAP 2 zrealizowany w kolejnym kroku.

## ETAP 2 — aplikacja Android (ukończony)

Cel: minimalny, użyteczny klient prywatnej karty pacjenta.

- aplikacja Compose w istniejącym module `app/` (Material 3, Navigation Compose),
- ekran logowania i rejestracji,
- bezpieczne przechowywanie tokenów: AES-GCM + klucz w Android Keystore,
- klient HTTP: Retrofit + Gson + OkHttp,
- automatyczne odświeżanie access tokenu i wylogowanie przy `401`,
- konfigurowalny adres backendu (bez hardkodowania IP), czyszczenie sesji przy zmianie,
- profil pacjenta: podgląd i edycja,
- listy i formularze: alergie, schorzenia, leki, objawy, pomiary, wizyty, lekarze,
- upload dokumentów i zdjęć przez SAF, pobieranie i otwieranie przez FileProvider,
- podgląd zdjęcia i analiza w trybie MOCK,
- czat asystenta w trybie MOCK z historią i źródłami,
- obsługa błędów sieci i komunikaty po polsku,
- nawigacja, stany ładowania, puste i błędu,
- 28 testów jednostkowych JVM, build `assembleDebug` zakończony sukcesem.

Poza zakresem ETAPU 2 (zrealizowane później lub świadomie pominięte): rzeczywiste AI,
OCR, powiadomienia, tryb offline, paginacja UI, testy instrumentowane.

**Status:** zakończony. Szczegóły: [`android.md`](android.md).

## ETAP 3 — dopracowanie doświadczenia pacjenta (następny)

Kandydaci (do potwierdzenia):

- dashboard/chronologia zdarzeń,
- wyszukiwanie i filtry,
- wykresy pomiarów,
- lepsze zarządzanie zdjęciami i seriami,
- eksport danych do PDF/CSV,
- motyw, dostępność, tryb ciemny,
- paginacja i sortowanie list,
- testy instrumentowane i CI.

## ETAP 4 — lokalny model AI (LM Studio)

> **Tu będzie potrzebny rzeczywisty model AI.** ETAPY 2–3 nie wymagają modelu.

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
