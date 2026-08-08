# Okeho Mapping ProGuard Rules

# Keep Supabase
-keep class io.github.jan.supabase.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep Hilt
-keep class dagger.hilt.** { *; }
