// supabase/functions/admin-data/index.ts
// Secure proxy: holds the service_role key server-side.
// The dashboard calls this with an admin password — never touches the key.

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  // CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  // Verify admin password
  const authHeader = req.headers.get("Authorization") || "";
  const token = authHeader.replace("Bearer ", "");
  const adminPassword = Deno.env.get("ADMIN_PASSWORD");

  if (!adminPassword || token !== adminPassword) {
    return new Response(
      JSON.stringify({ error: "Unauthorized" }),
      { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }

  // Init Supabase with service_role (bypasses RLS)
  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const supabase = createClient(supabaseUrl, serviceRoleKey);

  try {
    // Parse optional query params for filtering
    const url = new URL(req.url);
    const featureType = url.searchParams.get("feature_type");

    // Fetch captures
    let capturesQuery = supabase
      .from("captures")
      .select("id, name, feature_type, geometry, accuracy, photo_url, ocr_text, created_at, user_id");
    if (featureType && featureType !== "all") {
      capturesQuery = capturesQuery.eq("feature_type", featureType);
    }
    const { data: captures, error: capErr } = await capturesQuery;

    if (capErr) throw capErr;

    // Fetch streets
    const { data: streets, error: strErr } = await supabase
      .from("streets")
      .select("id, name, geometry, surface_type, traffic_direction, points_captured, created_at, user_id");

    if (strErr) throw strErr;

    // Generate signed URLs for photos that are object paths (not content:// URIs)
    const capturesWithPhotos = await Promise.all(
      (captures || []).map(async (c) => {
        if (c.photo_url && !c.photo_url.startsWith("content://") && !c.photo_url.startsWith("file://")) {
          const { data } = await supabase.storage
            .from("photos")
            .createSignedUrl(c.photo_url, 3600);
          return { ...c, photo_signed_url: data?.signedUrl || null };
        }
        return { ...c, photo_signed_url: null };
      })
    );

    return new Response(
      JSON.stringify({ captures: capturesWithPhotos, streets: streets || [] }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (e) {
    return new Response(
      JSON.stringify({ error: e.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
