# Add project specific ProGuard rules here.
# Cortex ships with minification off by default (see app/build.gradle.kts).
# When it's enabled for a release build, keep Room entities and kotlinx.serialization
# models since their field names/annotations are relied on by generated code.
-keep class com.cortex.app.data.db.entity.** { *; }
-keep class com.cortex.app.data.export.** { *; }
