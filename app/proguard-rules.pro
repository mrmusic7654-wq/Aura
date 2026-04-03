# Keep llmedge classes
-keep class com.Aatricks.llmedge.** { *; }
-dontwarn com.Aatricks.llmedge.**

# Keep model classes
-keep class com.aura.ai.model.** { *; }
-keep class com.aura.ai.data.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Compose
-keep class androidx.compose.** { *; }
