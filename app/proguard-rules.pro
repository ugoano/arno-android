# Keep kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class network.arno.android.**$$serializer { *; }
-keepclassmembers class network.arno.android.** {
    *** Companion;
}
-keepclasseswithmembers class network.arno.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}
