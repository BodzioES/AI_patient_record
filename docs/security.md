# Bezpieczeństwo

## 1. Model zagrożeń

System jest projektem lokalnym: backend i PostgreSQL działają na PC, a telefon
łączy się w prywatnej sieci Wi-Fi. Nie ma publicznego wystawienia usług.
Najważniejsze ryzyka:

- dostęp do danych medycznych przez zgubiony/pożyczony telefon,
- przejęcie tokenu przez podsłuch w niezabezpieczonej sieci,
- próba odczytu cudzych zasobów przez podstawienie identyfikatora,
- path traversal i upload złośliwego pliku,
- wyciek sekretów lub danych medycznych do repozytorium/logów,
- błąd konfiguracji wystawiający PostgreSQL lub pliki na sieć.

To nie jest produkt medyczny ani system o potwierdzonej zgodności z RODO.
Warstwa prawna i audyt bezpieczeństwa nie są częścią ETAPU 1.

## 2. Uwierzytelnianie

### Hasła

- BCrypt, strength 12.
- Minimum 8, maksimum 72 znaki (limit BCrypt).
- Hash nigdy nie jest zwracany w API.
- Błędny email i błędne hasło dają ten sam komunikat `401`.

### Access token

- JWT HS256, czas życia 15 minut (`JWT_ACCESS_TTL`).
- Zawiera `sub` (UUID użytkownika), `email`, `iss`, `iat`, `exp`.
- Podpis weryfikowany przy każdym żądaniu przez `JwtAuthenticationFilter`.
- Brak w tokenie danych medycznych.
- Brak możliwości unieważnienia pojedynczego access tokenu przed `exp`.

### Refresh token

- Losowy, nieprzezroczysty token (48 bajtów, Base64URL).
- W bazie przechowywany jest wyłącznie SHA-256.
- Czas życia 30 dni (`JWT_REFRESH_TTL`).
- Rotacja przy każdym `/refresh`; stary token jest unieważniany.
- `/logout` unieważnia refresh token.
- Ponowne użycie unieważnionego tokenu → `401`.

### Sekret JWT

- Wymagane co najmniej 32 bajty.
- Domyślna wartość w `application.yml` jest **tylko deweloperska** i powoduje
  ostrzeżenie w logu.
- W realnym użyciu ustaw `JWT_SECRET` losowym sekretem, np.
  `openssl rand -base64 48`.
- Sekretu nie wolno commitować; `.env` jest w `.gitignore`.

### Aplikacja Android (ETAP 2)

- Access i refresh token oraz adres serwera są szyfrowane AES-GCM kluczem 256-bit
  przechowywanym w Android Keystore (alias `patient_card_session_aes`).
- Brak plaintextu tokenów w SharedPreferences i w logach.
- `allowBackup=false` — dane aplikacji nie trafiają do backupu systemowego.
- Zmiana adresu serwera czyści tokeny; nie ma możliwości wysłania tokenu z jednego
  serwera do drugiego.
- Nieudany refresh lub `401` czyści sesję i wymusza ponowne logowanie.
- HTTP (cleartext) jest dozwolony tylko dla prywatnej sieci deweloperskiej; konfiguracja
  jest w `network_security_config.xml`. Publiczne wdrożenie wymaga HTTPS (ETAP 6).
- Logowanie OkHttp w debugowaniu używa poziomu `BASIC` — bez treści żądań i odpowiedzi.
- Pliki wybierane są przez SAF; aplikacja nie prosi o dostęp do całej pamięci.
  Pobrane pliki trafiają do `cacheDir` i są udostępniane przez `FileProvider`.
- Asystent AI i analiza zdjęcia zawsze pokazują zastrzeżenie medyczne; tryb pracy
  (MOCK albo model lokalny) pochodzi z `GET /api/v1/ai/status` i nie jest zgadywany
  po stronie aplikacji.

## 3. Autoryzacja

- `UserPrincipal` powstaje wyłącznie z podpisanego JWT.
- `CurrentPatientService` mapuje `user_id` → `patient_profile.id`.
- DTO nie zawierają `userId` ani `patientId`.
- Każde zapytanie o zasób potomny używa `findByIdAndPatientId(...)`.
- Cudzy zasób → `404`, nie `403` (brak ujawniania, że rekord istnieje).
- Powiązania opcjonalne (np. `symptomId`, `doctorId`, `sourceDocumentId`) są
  sprawdzane pod kątem właściciela przed zapisem; naruszenie → `400`.
- `ai_message_sources` jest polimorficzne, więc własność jest walidowana w
  `AiSourceValidator` przed zapisem.
- Brak ról i współdzielenia karty w ETAPIE 1.

## 4. Pliki

- Pliki są poza katalogiem statycznym; brak publicznego URL.
- `storage_key` generuje serwer; nazwa od użytkownika nie trafia do ścieżki.
- Dozwolone rozszerzenia:
  - zdjęcia: `jpg`, `jpeg`, `png`, `webp`,
  - dokumenty: dodatkowo `pdf`.
- Sprawdzany jest deklarowany MIME **oraz** sygnatura początku pliku (magic
  bytes). Niezgodność → `415 INVALID_FILE`.
- Limit: 25 MB na plik, 30 MB na żądanie (`MAX_FILE_SIZE`, `MAX_REQUEST_SIZE`).
- Pobieranie odbywa się przez kontroler z sprawdzeniem właściciela.
- Nagłówki pobierania: `Content-Disposition`, `X-Content-Type-Options: nosniff`,
  `Cache-Control: no-store`.
- Zapis: plik → rejestracja sprzątania przy rollbacku → rekord w DB → commit.
- Usuwanie: rekord w DB → commit → usunięcie pliku.
- Brak automatycznego sweepa osieroconych plików (patrz ograniczenia).

## 5. Baza danych

- PostgreSQL w Docker Compose wystawiony tylko na `127.0.0.1:5432`.
- Schemat wyłącznie przez Flyway; Hibernate `ddl-auto: validate`.
- UUID jako PK; brak przewidywalnych identyfikatorów sekwencyjnych w API.
- Więzy `UNIQUE`, `CHECK`, `FOREIGN KEY` chronią integralność.
- `TIMESTAMPTZ` i strefa UTC.
- Dane medyczne mają `patient_id` i kaskadowe usuwanie.
- Aplikacja używa konta bez uprawnień superużytkownika.

## 6. Walidacja i błędy

- Walidacja DTO (`jakarta.validation`) na wejściu.
- Walidacja reguł biznesowych w serwisach.
- Centralny handler zwraca `ProblemDetail` z kodem, bez stack trace.
- `server.error.include-message` i `include-stacktrace` ustawione na `never`.
- Logi nie zawierają treści medycznych ani tokenów; logowany jest tylko typ
  błędu i ogólny komunikat.

## 7. CORS i sieć

- Domyślnie CORS wyłączony (`CORS_ALLOWED_ORIGINS` puste).
- Jeśli ustawiony, dozwolone są tylko podane originy, nagłówki `Authorization`,
  `Content-Type`, `Accept` i metody HTTP.
- Brak `allowCredentials`.
- W sieci lokalnej nadal zalecany HTTPS przez reverse proxy (np. Caddy/Nginx)
  z zaufanym certyfikatem. ETAP 1 nie konfiguruje TLS.
- Nie wystawiaj backendu ani PostgreSQL na publiczny internet.

## 8. Sekrety i dane w repozytorium

- `.env` jest ignorowany.
- `local.properties`, buildy, pliki z `storage/` (poza `.gitkeep`) są ignorowane.
- `.env.example` zawiera wyłącznie placeholdery `CHANGE_ME`.
- Testy używają wyłącznie danych fikcyjnych i losowych emaili.
- Nie dodawaj prawdziwych zdjęć, dokumentów, wyników badań ani danych osobowych.

## 9. Znane ograniczenia

- Brak rate limitingu i blokady po wielu nieudanych logowaniach.
- Brak 2FA.
- Brak listy unieważnień access tokenów (logout nie unieważnia JWT).
- Brak szyfrowania plików na dysku (polegamy na szyfrowaniu dysku/OS).
- Brak szyfrowania bazy poza ewentualnym szyfrowaniem wolumenu.
- Brak pełnego audytu dostępu.
- Brak automatycznego czyszczenia osieroconych plików.
- Brak polityki retencji i eksportu danych.
- Brak testów penetracyjnych.

## 10. Checklist przed udostępnieniem w sieci

1. Ustaw silny `JWT_SECRET` i hasło bazy; nigdy nie używaj wartości domyślnych.
2. Włącz HTTPS na reverse proxy; nie wystawiaj `8080` bezpośrednio.
3. Ogranicz zaporą dostęp do PC i PostgreSQL.
4. Zrób kopię zapasową bazy i magazynu plików.
5. Sprawdź, że `storage/` i `.env` nie są w gicie.
6. Rozważ szyfrowanie dysku i silniejsze uwierzytelnianie.
7. Pamiętaj: to projekt studencki, nie narzędzie diagnostyczne.
