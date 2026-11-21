-- Zdjęcia dołączone do wiadomości czatu AI (lista UUID z medical_images, rozdzielona przecinkami).
-- Zdjęcia są zwykłymi rekordami medical_images, więc trafiają też do sekcji "Zdjęcia".
ALTER TABLE chat_messages ADD COLUMN image_ids VARCHAR(1000);
