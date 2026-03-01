# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities
-keep class com.fiozxr.yoursql.data.database.entity.** { *; }
-keep class com.fiozxr.yoursql.data.model.** { *; }
-keep class com.fiozxr.yoursql.domain.model.** { *; }

# Keep serialized classes
-keepattributes *Annotation*, InnerClasses
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Kotlin serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Netty
-keep class io.netty.** { *; }
-dontwarn io.netty.**
-dontwarn reactor.blockhound.**

# Bouncy Castle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Timber
-dontwarn org.jetbrains.annotations.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# General
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
