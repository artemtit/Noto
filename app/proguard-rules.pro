# Keep Room entities
-keep class com.noto.app.data.entity.** { *; }
# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.noto.app.**$$serializer { *; }
-keepclassmembers class com.noto.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.noto.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
