# ==============================================================================
# ProGuard / R8 Rules for FaceRecognition (AGP 9 / Target SDK 37)
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. GENERAL / RETRACE / LOGGING
# ------------------------------------------------------------------------------
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# Remove verbose/debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# ------------------------------------------------------------------------------
# 2. KOTLIN & COROUTINES
# ------------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keep,allowoptimization,allowshrinking,allowobfuscation class kotlin.coroutines.Continuation

# ------------------------------------------------------------------------------
# 3. KOTLINX SERIALIZATION
# ------------------------------------------------------------------------------
-dontnote kotlinx.serialization.AnnotationsKt

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keep class <1>$$serializer { *; }

# ------------------------------------------------------------------------------
# 4. DOMAIN ENTITIES & DATA CLASSES
# ------------------------------------------------------------------------------
-keep class com.example.detectionexample.models.** { *; }
-keepclassmembers class com.example.detectionexample.models.** {
    <fields>;
    <init>(...);
}

-keep class com.example.detectionexample.data.local.** { *; }
-keepclassmembers class com.example.detectionexample.data.local.** {
    <fields>;
    <init>(...);
}

-keep class com.example.detectionexample.core.model.** { *; }
-keepclassmembers class com.example.detectionexample.core.model.** {
    <fields>;
    <init>(...);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ------------------------------------------------------------------------------
# 5. ROOM DATABASE & DAOS
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep class * extends androidx.room.EntityDeletionOrUpdateAdapter { *; }
-keep class * extends androidx.room.EntityInsertionAdapter { *; }
-keep class * extends androidx.room.SharedSQLiteStatement { *; }
-keep class *_Impl { *; }
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# 6. DAGGER HILT
# ------------------------------------------------------------------------------
-dontwarn dagger.hilt.internal.**
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class com.example.detectionexample.**.*_HiltModules* { *; }

# ------------------------------------------------------------------------------
# 7. COMPOSE RUNTIME STABILITY
# ------------------------------------------------------------------------------
-keep @androidx.compose.runtime.Stable class **
-keep @androidx.compose.runtime.Immutable class **
-keepclassmembers class * {
    @androidx.compose.runtime.Stable <methods>;
}

# ------------------------------------------------------------------------------
# 8. JNI & NATIVE BINDINGS (OpenCV, C++ runtime)
# ------------------------------------------------------------------------------
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class org.opencv.** { *; }
-keepclassmembers class org.opencv.** { *; }
-dontwarn org.opencv.**

# ------------------------------------------------------------------------------
# 9. LITERT / TENSORFLOW LITE
# ------------------------------------------------------------------------------
-keep class com.google.ai.edge.litert.** { *; }
-keepclassmembers class com.google.ai.edge.litert.** { *; }
-dontwarn com.google.ai.edge.litert.**

-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# ------------------------------------------------------------------------------
# 10. ANDROIDX APPFUNCTIONS
# ------------------------------------------------------------------------------
-keep class androidx.appfunctions.** { *; }
-keep class com.example.detectionexample.appfunctions.** { *; }
-keepclassmembers class com.example.detectionexample.appfunctions.** {
    <fields>;
    <init>(...);
}

# ------------------------------------------------------------------------------
# 11. LITERT-LM
# ------------------------------------------------------------------------------
-keep class com.google.ai.edge.litertlm.** { *; }
-keepclassmembers class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**
-keep class com.example.detectionexample.chat.** { *; }

# ------------------------------------------------------------------------------
# 12. ML KIT GENAI PROMPT API & STRUCTURED OUTPUT
# ------------------------------------------------------------------------------
-keep class com.google.mlkit.genai.** { *; }
-keepclassmembers class com.google.mlkit.genai.** { *; }
-dontwarn com.google.mlkit.genai.**
-keep class com.example.detectionexample.genai.** { *; }
-keepclassmembers class com.example.detectionexample.genai.** {
    <fields>;
    <init>(...);
}

# ------------------------------------------------------------------------------
# 13. ML KIT GENAI AGENT API
# ------------------------------------------------------------------------------
-keep class com.example.detectionexample.agent.** { *; }
-keepclassmembers class com.example.detectionexample.agent.** {
    <fields>;
    <init>(...);
}

# ------------------------------------------------------------------------------
# 14. JETPACK GLANCE & ACTION CALLBACKS
# ------------------------------------------------------------------------------
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}
