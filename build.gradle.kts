// Kotlin Gradle plugin is declared here (not applied) so the whole build uses Kotlin 2.4.21;
// AGP 9 compiles Kotlin itself (built-in Kotlin), so app/ does not apply kotlin-android.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
