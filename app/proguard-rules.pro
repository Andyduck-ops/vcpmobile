# Keep Kotlinx Serialization models
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# Keep Room entities and DAO metadata
-keep class androidx.room.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep Firebase messaging service
-keep class com.google.firebase.messaging.FirebaseMessagingService { *; }
