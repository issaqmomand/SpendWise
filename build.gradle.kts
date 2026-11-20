
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Firebase
    id("com.google.gms.google-services") version "4.4.2" apply false

    // Room database annotation processing
    id("com.google.devtools.ksp") version "2.3.12" apply false
}
