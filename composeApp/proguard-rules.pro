# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclassmembers class kotlinx.serialization.json.**$* {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keep,includedescriptorclasses class pl.deniotokiari.tickerwire.model.**$$serializer { *; }
-keepclassmembers class pl.deniotokiari.tickerwire.model.** {
    *** Companion;
}
-keepclasseswithmembers class pl.deniotokiari.tickerwire.model.** {
    *** Companion;
}
-keepclassmembers class pl.deniotokiari.tickerwire.model.** {
    <fields>;
}
-keepclassmembers class pl.deniotokiari.tickerwire.model.dto.** {
    <fields>;
}
-keepclassmembers class pl.deniotokiari.tickerwire.common.data.cache.CacheEntry {
    <fields>;
}

# Keep all @Serializable classes
-keep @kotlinx.serialization.Serializable class * {
    <fields>;
}

# Keep serializers
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    <init>(...);
}

# Ktor Client
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Ktor Serialization
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class kotlinx.serialization.json.** {
    *** INSTANCE;
    *** getInstance();
}
-keepclassmembers class kotlinx.serialization.json.Json {
    *** INSTANCE;
    *** getInstance();
}

# Koin Dependency Injection
-keep class org.koin.** { *; }
-dontwarn org.koin.**
-keepclassmembers class * {
    @org.koin.core.annotation.Single <methods>;
    @org.koin.core.annotation.Factory <methods>;
}
-keep @org.koin.core.annotation.Single class *
-keep @org.koin.core.annotation.Factory class *
-keepclassmembers class * {
    @org.koin.core.annotation.Named <methods>;
}

# Compose Multiplatform
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.** { *; }

# Navigation Compose
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ViewModel
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class pl.deniotokiari.tickerwire.feature.**.presentation.*ViewModel {
    <init>(...);
}

# Firebase Analytics
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Multiplatform Settings
-keep class com.russhwolf.settings.** { *; }
-dontwarn com.russhwolf.settings.**

# Kermit Logging
-keep class co.touchlab.kermit.** { *; }
-dontwarn co.touchlab.kermit.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep generic signatures
-keepattributes Signature

# Keep line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep data classes used in serialization
-keep class pl.deniotokiari.tickerwire.model.** { *; }
-keep class pl.deniotokiari.tickerwire.model.dto.** { *; }

# Keep classes with @Serializable annotation
-keep @kotlinx.serialization.Serializable class pl.deniotokiari.tickerwire.** { *; }

# Keep CacheEntry and its serializer
-keep class pl.deniotokiari.tickerwire.common.data.cache.CacheEntry { *; }
-keep class pl.deniotokiari.tickerwire.common.data.cache.CacheEntry$$serializer { *; }

# Keep HttpClient and related classes
-keep class pl.deniotokiari.tickerwire.common.data.HttpClient { *; }

# Keep repositories
-keep class pl.deniotokiari.tickerwire.common.data.**Repository { *; }
-keep class pl.deniotokiari.tickerwire.common.data.store.** { *; }

# Keep UseCases
-keep class pl.deniotokiari.tickerwire.feature.**.domain.**UseCase { *; }

# Keep ViewModels
-keep class pl.deniotokiari.tickerwire.feature.**.presentation.*ViewModel { *; }

# Keep MainActivity
-keep class pl.deniotokiari.tickerwire.MainActivity { *; }

# Keep App class
-keep class pl.deniotokiari.tickerwire.feature.app.presentation.App { *; }

# Remove logging in release
-assumenosideeffects class kotlin.io.Console {
    public static void println(...);
    public static void print(...);
}

# OkHttp (used by Ktor Android engine)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep reflection for serialization
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**
-dontwarn javax.activation.**
-dontwarn javax.inject.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit
-dontwarn kotlin.jvm.internal.**
-dontwarn kotlin.reflect.**

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

