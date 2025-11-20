# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Realm Kotlin
-keep class io.realm.kotlin.** { *; }
-keep class io.realm.kotlin.internal.** { *; }
-dontwarn io.realm.kotlin.**
-keep @io.realm.kotlin.types.RealmObject class *
-keep class * implements io.realm.kotlin.types.RealmObject {
    public protected *;
}

# Keep data model classes
-keep class com.aksara.notes.data.database.entities.** { *; }
-keep class com.aksara.notes.data.models.** { *; }

# Security Crypto
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Biometric
-keep class androidx.biometric.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# Keep ViewBinding classes
-keep class com.aksara.notes.databinding.** { *; }

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# OkHttp optional dependencies (used by Realm Sync)
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**

# Missing annotation libraries (Tink dependencies)
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**