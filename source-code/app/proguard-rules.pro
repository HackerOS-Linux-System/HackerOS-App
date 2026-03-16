-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.hackeros.app.data.model.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
