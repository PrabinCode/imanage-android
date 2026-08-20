# Proguard rules for I Manage
-keepattributes *Annotation*
-dontwarn java.lang.management.**
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
