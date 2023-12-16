# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


###

-keep class com.example.priceless.User { *; }
-keepclassmembers class com.example.priceless.User { *; }
-keepclassmembernames class com.example.priceless.User { *; }

-keep class com.example.priceless.FollowRequest { *; }
-keepclassmembers class com.example.priceless.FollowRequest { *; }
-keepclassmembernames class com.example.priceless.FollowRequest { *; }

-keep class com.example.priceless.PostStructure { *; }
-keepclassmembers class com.example.priceless.PostStructure { *; }
-keepclassmembernames class com.example.priceless.PostStructure { *; }

-keep class com.example.priceless.CommentStructure { *; }
-keepclassmembers class com.example.priceless.CommentStructure { *; }
-keepclassmembernames class com.example.priceless.CommentStructure { *; }

-keep class com.example.priceless.GetTime { *; }
-keepclassmembers class com.example.priceless.GetTime { *; }
-keepclassmembernames class com.example.priceless.GetTime { *; }

###


-printconfiguration /tmp/full-r8-config.txt
# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
#-dontwarn org.bouncycastle.jsse.BCSSLParameters
#-dontwarn org.bouncycastle.jsse.BCSSLSocket
#-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
#-dontwarn org.conscrypt.Conscrypt$Version
#-dontwarn org.conscrypt.Conscrypt
#-dontwarn org.conscrypt.ConscryptHostnameVerifier
#-dontwarn org.openjsse.javax.net.ssl.SSLParameters
#-dontwarn org.openjsse.javax.net.ssl.SSLSocket
#-dontwarn org.openjsse.net.ssl.OpenJSSE
-keep class org.bouncycastle.** { *; }
-keep class org.conscrypt.** { *; }
-keep class org.openjsse.** { *; }


# okhttp rules
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# A resource is loaded with a relative path so the package of this class must be preserved.
-adaptresourcefilenames okhttp3/internal/publicsuffix/PublicSuffixDatabase.gz

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**