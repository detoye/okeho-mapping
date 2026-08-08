-- Create storage bucket for photos
INSERT INTO storage.buckets (id, name, public)
VALUES ('photos', 'photos', false);

-- Storage policy: users can upload to their own folder
CREATE POLICY "Users can upload photos" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'photos'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

-- Storage policy: users can view their own photos
CREATE POLICY "Users can view own photos" ON storage.objects
    FOR SELECT USING (
        bucket_id = 'photos'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

-- Storage policy: users can delete their own photos
CREATE POLICY "Users can delete own photos" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'photos'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );
