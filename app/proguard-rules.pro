# Firestore maps documents onto DTOs via reflection (toObject) — R8 must not
# rename or strip their fields/constructors, or every mapped field silently
# falls back to its default value.
-keep class com.triptogether.core.data.dto.** { *; }
-keepclassmembers class com.triptogether.core.data.dto.** { <init>(); }

# Firestore annotations used on DTOs (ServerTimestamp etc.).
-keepattributes *Annotation*

# kotlinx.datetime parses via its own serializers; keep its public API surface.
-dontwarn kotlinx.datetime.**

# Crashlytics: keep file names/line numbers readable in reports.
-keepattributes SourceFile,LineNumberTable
