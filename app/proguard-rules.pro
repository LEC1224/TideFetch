# yt-dlp embeds Python and discovers parts of its runtime dynamically.
-keep class com.yausername.youtubedl_android.** { *; }
-dontwarn com.yausername.youtubedl_android.**

# Keep coroutine continuation metadata used in optimized release builds.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
