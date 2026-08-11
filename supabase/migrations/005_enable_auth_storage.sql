-- Prepare for real auth: photo upload to Storage, and a signup path that works.
--
-- Deliberately does NOT re-tighten captures/streets. The APK in the field still
-- sends user_id = NULL, and a policy requiring auth.uid() = user_id would break
-- it the moment this migration lands -- a migration applies instantly, a new
-- APK is ~7 minutes of CI plus a manual install. Tightening lives in 006, after
-- the auth build is verified on-device.
--
-- The three storage policies from 002 need no change here: they key on
-- (storage.foldername(name))[1] = auth.uid()::text, which becomes satisfiable
-- on its own once auth.uid() is non-null and the object path starts with the
-- user's UUID. Only UPDATE was missing.

-- Storage policy: users can overwrite their own photos.
-- Storage implements upsert as INSERT-or-UPDATE, so without this an upsert
-- fails even when authenticated. Sync needs it: the object path is
-- deterministic (<uid>/<captureId>.jpg) so that re-syncing a FAILED capture
-- overwrites in place instead of accumulating orphaned duplicates.
CREATE POLICY "Users can update own photos" ON storage.objects
    FOR UPDATE
    USING (
        bucket_id = 'photos'
        AND (storage.foldername(name))[1] = auth.uid()::text
    )
    WITH CHECK (
        bucket_id = 'photos'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

-- Harden the profile trigger from 001. It has existed since the first
-- migration but has never once fired, because no user has ever signed up.
-- It is now on the critical path for every signup, and an exception inside a
-- BEFORE/AFTER trigger on auth.users aborts the whole INSERT -- surfacing to
-- the client as an opaque 500 "Database error saving new user" with no
-- indication that a profiles row was the cause. Failing to create a profile
-- should never cost the user their account.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, full_name)
    VALUES (NEW.id, NEW.raw_user_meta_data->>'full_name')
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'handle_new_user failed for %: %', NEW.id, SQLERRM;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
