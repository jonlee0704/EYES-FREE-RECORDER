# Proguard / R8 rules for SJPlayer 4
# Keep Google Play Services & Auth
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }

# Keep Google Drive REST API & HTTP Client
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.model.** { *; }
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}

# Keep GSON models and annotations
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.jonlee.android.SJplayer.BackupFileResult { *; }

# AndroidX MultiDex
-keep class androidx.multidex.** { *; }
