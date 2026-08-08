# Okeho Mapping - Setup Guide

## 1. Create Supabase Project

1. Go to https://supabase.com and sign up/login
2. Click "New Project"
3. Enter project name: `okeho-mapping`
4. Set a database password (save it!)
5. Choose region closest to Nigeria (e.g., EU West)
6. Click "Create new project"

## 2. Get Your Credentials

1. In your project dashboard, go to **Settings** → **API**
2. Copy:
   - **Project URL** (looks like: `https://xxxx.supabase.co`)
   - **anon public** key (long string starting with `eyJ...`)

## 3. Configure the App

Open `android/app/src/main/java/com/okeho/mapping/Config.kt` and replace:

```kotlin
const val SUPABASE_URL = "YOUR_SUPABASE_URL"        // ← Paste your Project URL
const val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"  // ← Paste your anon key
```

## 4. Run the Migrations

1. In Supabase dashboard, go to **SQL Editor**
2. Click "New query"
3. Paste the contents of `supabase/migrations/001_initial_schema.sql`
4. Click "Run"
5. Create another query
6. Paste `supabase/migrations/002_storage_buckets.sql`
7. Click "Run"

## 5. Build the Android App

```bash
cd android
./gradlew assembleDebug
```

The APK will be at `android/app/build/outputs/apk/debug/app-debug.apk`

## 6. Create a Test Account

1. In Supabase dashboard, go to **Authentication** → **Users**
2. Click "Add user"
3. Enter email and password
4. Use these credentials to log in to the app

## Project Structure

```
Okeho/
├── supabase/
│   └── migrations/          # SQL schema files
├── android/
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/okeho/mapping/
│   │   │   │   ├── data/       # Room DB, Supabase, sync
│   │   │   │   ├── domain/     # Models, repositories
│   │   │   │   ├── ui/         # Screens, theme
│   │   │   │   └── di/         # Hilt modules
│   │   │   └── res/            # Android resources
│   │   └── build.gradle.kts
│   └── build.gradle.kts
├── SETUP.md
└── Okeho_Mapping_App_UI_UX_Plan.pdf
```
