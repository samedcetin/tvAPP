# JavaScript bridges used by the embedded YouTube / web players.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# kotlinx.serialization: keep generated serializers of our models.
-keepclassmembers @kotlinx.serialization.Serializable class tr.ailetv.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class tr.ailetv.**$$serializer { *; }
