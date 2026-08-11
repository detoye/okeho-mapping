-- Restore per-user access control, reversing the PoC relaxations in 003/004.
--
-- Safe to apply now: there is no installed build still sending user_id = NULL.
-- The auth build stamps a real UUID on every insert, so requiring
-- auth.uid() = user_id no longer locks anyone out.
--
-- This matters beyond posture. The app now READS from Supabase to restore
-- records after a reinstall, and with 003's `USING (true)` SELECT policies
-- still in place every user's Records screen would list every other user's
-- captures.

-- The three pre-auth rows (2 captures, 1 street) predate identity: user_id is
-- NULL, so they satisfy no owner policy and the NOT NULL below would reject
-- them outright. Their photo_url values are device-local content:// URIs that
-- resolve nowhere, so there is nothing recoverable to reassign.
DELETE FROM public.captures WHERE user_id IS NULL;
DELETE FROM public.streets  WHERE user_id IS NULL;

-- Drop 003's blanket policies.
DROP POLICY IF EXISTS "Allow all inserts captures" ON public.captures;
DROP POLICY IF EXISTS "Allow all selects captures" ON public.captures;
DROP POLICY IF EXISTS "Allow all updates captures" ON public.captures;
DROP POLICY IF EXISTS "Allow all deletes captures" ON public.captures;
DROP POLICY IF EXISTS "Allow all inserts streets"  ON public.streets;
DROP POLICY IF EXISTS "Allow all selects streets"  ON public.streets;
DROP POLICY IF EXISTS "Allow all updates streets"  ON public.streets;
DROP POLICY IF EXISTS "Allow all deletes streets"  ON public.streets;

-- Restore 001's per-user policies. Dropped first so this migration can be
-- re-run without colliding on names.
DROP POLICY IF EXISTS "Users can view own captures"   ON public.captures;
DROP POLICY IF EXISTS "Users can insert own captures" ON public.captures;
DROP POLICY IF EXISTS "Users can update own captures" ON public.captures;
DROP POLICY IF EXISTS "Users can delete own captures" ON public.captures;

CREATE POLICY "Users can view own captures" ON public.captures
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own captures" ON public.captures
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update own captures" ON public.captures
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can delete own captures" ON public.captures
    FOR DELETE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can view own streets"   ON public.streets;
DROP POLICY IF EXISTS "Users can insert own streets" ON public.streets;
DROP POLICY IF EXISTS "Users can update own streets" ON public.streets;
DROP POLICY IF EXISTS "Users can delete own streets" ON public.streets;

CREATE POLICY "Users can view own streets" ON public.streets
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own streets" ON public.streets
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update own streets" ON public.streets
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can delete own streets" ON public.streets
    FOR DELETE USING (auth.uid() = user_id);

-- Note on UPDATE: 001 wrote these with USING only. Without WITH CHECK, a user
-- could pass the row-visibility test and then reassign user_id to someone
-- else, handing away their own record. Both clauses are required.

ALTER TABLE public.captures ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE public.streets  ALTER COLUMN user_id SET NOT NULL;

-- Re-add the FK, but to auth.users rather than 001's public.profiles.
-- Pointing at profiles put handle_new_user on the critical path of every
-- insert: if that trigger had not yet created the profile row, the insert
-- failed with 23503. That is the failure 004 was written to escape.
-- auth.users is the real identity table and is populated before any insert
-- can be attempted.
ALTER TABLE public.captures
    ADD CONSTRAINT captures_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;

ALTER TABLE public.streets
    ADD CONSTRAINT streets_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;
