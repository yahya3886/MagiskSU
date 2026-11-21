# MagiskSU ProGuard rules
# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# DataStore / serialization
-keepclassmembers class * extends androidx.datastore.core.DataStore { *; }
