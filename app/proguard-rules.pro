# yt-dlp embeds Python and discovers parts of its runtime dynamically.
-keep class com.yausername.youtubedl_android.** { *; }
-dontwarn com.yausername.youtubedl_android.**

# Commons Compress registers ZIP extra-field implementations reflectively while
# unpacking the portable Python runtime. Preserve those constructors and names;
# without this, a minified APK can fail at startup with an opaque name like e3.f.
-keep class org.apache.commons.compress.archivers.zip.** { *; }

# Keep dependency exception names useful in the user-copyable technical log.
-keep class com.yausername.ffmpeg.** { *; }
-keep class com.yausername.youtubedl_common.** { *; }

# Keep coroutine continuation metadata used in optimized release builds.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
