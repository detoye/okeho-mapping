-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Users table (extends Supabase auth.users)
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name VARCHAR(255),
    phone VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Captures table (point features)
CREATE TABLE public.captures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    name VARCHAR(255),
    feature_type VARCHAR(50) NOT NULL CHECK (feature_type IN (
        'business', 'landmark', 'school', 'health_facility',
        'building', 'transport', 'road_feature', 'other'
    )),
    geometry GEOMETRY(POINT, 4326) NOT NULL,
    accuracy FLOAT,
    photo_url VARCHAR(500),
    ocr_text VARCHAR(500),
    sync_status VARCHAR(20) DEFAULT 'pending' CHECK (sync_status IN ('pending', 'synced', 'failed')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Streets table (line features)
CREATE TABLE public.streets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    name VARCHAR(255),
    geometry GEOMETRY(LINESTRING, 4326) NOT NULL,
    surface_type VARCHAR(20) CHECK (surface_type IN ('paved', 'unpaved')),
    traffic_direction VARCHAR(20) CHECK (traffic_direction IN ('one_way', 'two_way')),
    points_captured INTEGER DEFAULT 0,
    sync_status VARCHAR(20) DEFAULT 'pending' CHECK (sync_status IN ('pending', 'synced', 'failed')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for spatial queries
CREATE INDEX idx_captures_geometry ON public.captures USING GIST (geometry);
CREATE INDEX idx_streets_geometry ON public.streets USING GIST (geometry);
CREATE INDEX idx_captures_user_id ON public.captures (user_id);
CREATE INDEX idx_streets_user_id ON public.streets (user_id);
CREATE INDEX idx_captures_sync_status ON public.captures (sync_status);
CREATE INDEX idx_streets_sync_status ON public.streets (sync_status);

-- RLS policies
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.captures ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.streets ENABLE ROW LEVEL SECURITY;

-- Profiles: users can read/update their own profile
CREATE POLICY "Users can view own profile" ON public.profiles
    FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Users can update own profile" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile" ON public.profiles
    FOR INSERT WITH CHECK (auth.uid() = id);

-- Captures: users can CRUD their own captures
CREATE POLICY "Users can view own captures" ON public.captures
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own captures" ON public.captures
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own captures" ON public.captures
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own captures" ON public.captures
    FOR DELETE USING (auth.uid() = user_id);

-- Streets: users can CRUD their own streets
CREATE POLICY "Users can view own streets" ON public.streets
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own streets" ON public.streets
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own streets" ON public.streets
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own streets" ON public.streets
    FOR DELETE USING (auth.uid() = user_id);

-- Function to auto-create profile on signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, full_name)
    VALUES (NEW.id, NEW.raw_user_meta_data->>'full_name');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger for new user signup
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Function to find nearby captures
CREATE OR REPLACE FUNCTION public.nearby_captures(
    lat FLOAT,
    lng FLOAT,
    radius_meters FLOAT DEFAULT 100
)
RETURNS TABLE (
    id UUID,
    name VARCHAR(255),
    feature_type VARCHAR(55),
    distance FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id,
        c.name,
        c.feature_type,
        ST_Distance(
            c.geometry::geography,
            ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography
        ) AS distance
    FROM public.captures c
    WHERE ST_DWithin(
        c.geometry::geography,
        ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography,
        radius_meters
    )
    ORDER BY distance;
END;
$$ LANGUAGE plpgsql;
