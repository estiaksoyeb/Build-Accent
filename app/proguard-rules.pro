# --- Accent Builder ProGuard Rules ---

# Keep data classes used for JSON serialization (Backups)
# We must keep field names so Gson reflection works correctly.
-keep class com.buildaccent.as.data.** { *; }
-keep class com.buildaccent.as.ui.studio.LessonSegment { *; }

# Gson general rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Room specific rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Compose specific stuff if needed (usually handled by Compose rules)
-keep class androidx.compose.ui.platform.AndroidComposeView { *; }