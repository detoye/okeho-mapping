-- PoC mode: allow records to sync without an authenticated user.
--
-- 003_relax_rls_for_poc.sql relaxed the RLS policies but left the FK to
-- profiles and the NOT NULL on user_id in place, so anonymous inserts still
-- failed with 23503 / 23502. This finishes that job.

ALTER TABLE public.captures DROP CONSTRAINT IF EXISTS captures_user_id_fkey;
ALTER TABLE public.streets  DROP CONSTRAINT IF EXISTS streets_user_id_fkey;

ALTER TABLE public.captures ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE public.streets  ALTER COLUMN user_id DROP NOT NULL;
