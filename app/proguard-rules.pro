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
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class com.jonlee.android.SJplayer.BackupFileResult { *; }

# AndroidX MultiDex & Preferences
-keep class androidx.multidex.** { *; }
-keep class androidx.preference.** { *; }
-keep class * extends androidx.preference.PreferenceFragmentCompat { *; }

# Keep Android custom views and XML-referenced components
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Suppress warnings for optional desktop Java dependencies in Apache HTTP & Google API Client
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.http.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
