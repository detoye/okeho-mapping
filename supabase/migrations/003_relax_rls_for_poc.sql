-- Fix RLS policies to allow anonymous inserts for PoC
-- Drop existing restrictive policies
DROP POLICY IF EXISTS "Users can insert own captures" ON public.captures;
DROP POLICY IF EXISTS "Users can view own captures" ON public.captures;
DROP POLICY IF EXISTS "Users can insert own streets" ON public.streets;
DROP POLICY IF EXISTS "Users can view own streets" ON public.streets;

-- Allow anyone to insert (for PoC without auth)
CREATE POLICY "Allow all inserts captures" ON public.captures
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Allow all selects captures" ON public.captures
    FOR SELECT USING (true);

CREATE POLICY "Allow all inserts streets" ON public.streets
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Allow all selects streets" ON public.streets
    FOR SELECT USING (true);

-- Allow updates and deletes too
CREATE POLICY "Allow all updates captures" ON public.captures
    FOR UPDATE USING (true);

CREATE POLICY "Allow all deletes captures" ON public.captures
    FOR DELETE USING (true);

CREATE POLICY "Allow all updates streets" ON public.streets
    FOR UPDATE USING (true);

CREATE POLICY "Allow all deletes streets" ON public.streets
    FOR DELETE USING (true);
