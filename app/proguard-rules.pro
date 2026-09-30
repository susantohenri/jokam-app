# Keep Kotlin Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class * {
    *** Companion;
}

-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}


-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep models in data package
-keep class com.jokam.tempatsambung.data.model.** { *; }
-keepclassmembers class com.jokam.tempatsambung.data.model.** { *; }
