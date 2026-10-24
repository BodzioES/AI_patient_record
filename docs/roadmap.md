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

## ETAP 3 — dopracowanie doświadczenia pacjenta (ukończony)

Zakres zrealizowany:

- wspólna chronologia zdarzeń: `GET /api/v1/app/timeline` scala wizyty, pomiary,
  objawy, leki, dokumenty i zdjęcia; filtr `types`, zakres `from`/`to`, paginacja,
- wyszukiwanie: `GET /api/v1/app/search` po dziewięciu sekcjach karty, trafienia
  sortowane najpierw po dopasowaniu tytułu, potem po dacie,
- wykresy pomiarów w aplikacji (Canvas, bez zewnętrznej biblioteki) ze statystykami,
- paginacja, sortowanie serwerowe i lokalny filtr tekstowy list,
- eksport całej karty do CSV i PDF przez SAF, bez wysyłania danych na zewnątrz,
- zdjęcia: filtr kategorii i grupowanie w serie po `symptomId`,
- dostępność: wybór rozmiaru tekstu w całej aplikacji (0,9–1,4),
- motyw jasny, szaro-biały z czerwonym akcentem (bez trybu ciemnego).

**Status:** zakończony. Testy jednostkowe ETAPU 3 nie były uruchamiane w tej sesji —
zostawione do samodzielnego wykonania przez użytkownika. Szczegóły:
[`android.md`](android.md) i [`api.md`](api.md).

## ETAP 4 — lokalny model AI (LM Studio) (ukończony)

Zakres zrealizowany:

- `LmStudioAiProvider` implementujący `AiProvider` (OpenAI-compatible
  `POST /v1/chat/completions`, bez streamingu),
- wybór dostawcy przez `AI_PROVIDER=mock|lmstudio`, bez zmian w serwisach i UI,
- reguły systemowe po polsku: rola „wirtualnego lekarza” — wstępna interpretacja
  wartości i prawdopodobne wyjaśnienia jako przypuszczenie, bez pewnej diagnozy,
  bez zmian dawek, objawy alarmowe → 112/SOR, każda odpowiedź kończy się
  zastrzeżeniem „to tylko przypuszczenia… skonsultuj z lekarzem lub specjalistą”,
- budżet promptu (`AI_MAX_PROMPT_CHARS`) i przycinanie historii do okna kontekstu,
- obsługa modeli rozumujących (`reasoning_effort`, pusty `content`, `finish_reason=length`),
- analiza zdjęć przez model vision (base64 data URL) + jawna analiza metadanych,
  gdy plik jest za duży lub model nie widzi obrazu,
- czytelne błędy `503 AI_PROVIDER_ERROR` po polsku (brak połączenia, timeout,
  brak modelu, autoryzacja, za duże zdjęcie),
- `GET /api/v1/ai/status` i dynamiczne etykiety w aplikacji (MOCK vs nazwa modelu),
- konfiguracja Docker → LM Studio przez `host.docker.internal`,
- dokumentacja: [`ai-architecture.md`](ai-architecture.md), [`development.md`](development.md).

**Status:** zakończony. Streaming odpowiedzi świadomie pominięty (model odpowiada
synchronicznie; aplikacja ma zwiększony timeout odczytu).

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
