# Keep Kotlin/Coroutines & JSON compatibility for R8
-dontwarn org.json.**
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
