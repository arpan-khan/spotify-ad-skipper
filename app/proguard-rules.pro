# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Shizuku API classes
-keep class moe.shizuku.** { *; }
-keep interface moe.shizuku.** { *; }
-keepclassmembers class moe.shizuku.** { *; }

# Keep Result sealed class and its subclasses
-keep class com.spotify.adskipper.Result { *; }
-keep class com.spotify.adskipper.Result$* { *; }

# Keep all app components (Activities, Services, etc.)
-keep class com.spotify.adskipper.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep notification listener service
-keep class * extends android.service.notification.NotificationListenerService {
    public <methods>;
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
