# ProGuard rules for HongBaoAssistant
# Keep accessibility service
-keep class com.simplehongbao.assistant.WxBaoAccessibilityService { *; }
-keep class com.simplehongbao.assistant.MainActivity { *; }

# Keep model classes if any
-keepclassmembers class * {
    @android.widget onBind <methods>;
}

# AndroidX
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
