import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

// Release builds take their version from the environment so the release workflow
// can stamp them from the git tag. Local/CI debug builds fall back to the
// checked-in defaults below.
val mimiVersionName = providers.environmentVariable("MIMI_VERSION_NAME").orNull
val mimiVersionCode = providers.environmentVariable("MIMI_VERSION_CODE").orNull?.toIntOrNull()

// MIMI's beta signing key. It is committed on purpose: it is a public beta key,
// not a secret, and having one stable key is what allows a newer APK to install
// over an older one. See tools/keystore/README.md.
val betaKeystore = rootProject.file("tools/keystore/mimi-beta.keystore")

android {
    namespace = "com.exodidio.mimi"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileSdkMinor = libs.versions.android.compileSdkMinor.get().toInt()

    defaultConfig {
        applicationId = "com.exodidio.mimi"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = mimiVersionCode ?: 1
        versionName = mimiVersionName ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (betaKeystore.exists()) {
            create("beta") {
                storeFile = betaKeystore
                storePassword = "mimibeta"
                keyAlias = "mimi-beta"
                keyPassword = "mimibeta"
            }
        }
    }

    buildTypes {
        release {
            // R8 is enabled once the beta-1 surface is stable. Kept off for now so
            // the first cloud builds stay predictable.
            isMinifyEnabled = false
            isShrinkResources = false
            if (betaKeystore.exists()) {
                signingConfig = signingConfigs.getByName("beta")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewmodelCompose)

    implementation(libs.compose.material3)
    implementation(libs.compose.lucideIcons)
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.compose.uiToolingPreview)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
