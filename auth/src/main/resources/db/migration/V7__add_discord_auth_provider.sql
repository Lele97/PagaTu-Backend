-- Estende il vincolo su users.auth_provider al valore GITHUB, introdotto con il
-- login OAuth GitHub. Senza questo, ogni INSERT con auth_provider='GITHUB' viene
-- rifiutato dal database.
--
-- Il vincolo precedente esiste solo su alcuni database (era stato creato a mano
-- durante lo sviluppo) e non era presente nelle migration V1-V6, quindi non si
-- può assumere né il nome né l'esistenza. Qui si rimuovono tutti i CHECK su
-- auth_provider, qualunque sia il loro nome, e se ne ricrea uno canonico.

-- 1. Rimuove ogni CHECK che vincola auth_provider.
DO $$
    DECLARE
        c RECORD;
    BEGIN
        FOR c IN
            SELECT conname
            FROM pg_constraint
            WHERE conrelid = 'users'::regclass
              AND contype = 'c'
              AND pg_get_constraintdef(oid) LIKE '%auth_provider%'
            LOOP
                EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', c.conname);
            END LOOP;
    END
$$;

-- 2. Ricrea il vincolo canonico sui quattro provider supportati.
ALTER TABLE users
    ADD CONSTRAINT users_auth_provider_check
        CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'DISCORD', 'GITHUB'));