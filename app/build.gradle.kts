import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Release signing values are read from environment variables first (used by CI),
 * then from a properties file outside the repository. Nothing secret is committed.
 */
val signingProperties = Properties().apply {
    val path = System.getenv("EGG_TIMER_SIGNING_PROPERTIES")
        ?: "${System.getProperty("user.home")}/.android-keys/keystore.properties"
    val file = file(path)
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun signingValue(property: String, environment: String): String? =
    System.getenv(environment) ?: signingProperties.getProperty(property)

android {
    namespace = "com.kram1nctrl.eggtimer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kram1nctrl.eggtimer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val keystore = signingValue("eggTimer.keystore", "EGG_TIMER_KEYSTORE")
            if (keystore != null && file(keystore).exists()) {
                storeFile = file(keystore)
                storePassword = signingValue("eggTimer.keystorePassword", "EGG_TIMER_KEYSTORE_PASSWORD")
                keyAlias = signingValue("eggTimer.keyAlias", "EGG_TIMER_KEY_ALIAS")
                keyPassword = signingValue("eggTimer.keyPassword", "EGG_TIMER_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
}
