# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep line numbers for readable stack traces in Play Console / Crashlytics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ─────────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
# Kotlinx Coroutines — used reflectively by the debug agent only, safe to keep small
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# ── Compose ───────────────────────────────────────────────────────────────────
# Compose keeps what it needs via its own rules shipped in the AARs. Only
# custom reflective access needs extra -keep lines — none in this project yet.
-dontwarn androidx.compose.**

# ── AndroidX Navigation Compose ──────────────────────────────────────────────
# NavType reflection for argument bundling
-keepnames class * extends androidx.navigation.NavType

# ── Media3 / ExoPlayer ────────────────────────────────────────────────────────
# Keep public APIs — several are reflectively instantiated (renderers, factories,
# extractors, decoders). Over-keeping is preferable to runtime crashes.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
# ExoPlayer extensions (IMA, Cast, etc.) referenced but not always present
-dontwarn com.google.android.exoplayer2.**

# ── Guava (bundled with media3-session via ListenableFuture) ─────────────────
-dontwarn com.google.common.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**

# ── WorkManager（Worker 子类通过反射实例化）─────────────────────────────────
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.example.player.work.** { *; }

# ── Coil ──────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-keep class coil.video.** { *; }
-dontwarn coil.**

# ── Volley ────────────────────────────────────────────────────────────────────
-keep class com.android.volley.** { *; }
-dontwarn com.android.volley.**

# ── Application entry points ─────────────────────────────────────────────────
# Application / Activity classes are already kept by android:name references
# in the Manifest — no extra -keep needed.

# ── Kotlin data classes used for JSON serialization (Folder / VideoItem) ─────
# We serialize via JSONObject by field name, so keep the data class members.
-keep class com.example.player.model.** { *; }

# ── Serializable / Parcelable guards ─────────────────────────────────────────
-keepnames class * implements java.io.Serializable
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ── Reflection on enums (SortOption) ─────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Remove debug-only log noise in release ───────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
