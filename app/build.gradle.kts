import java.util.Properties
import java.time.LocalDate

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

// Load local.properties (never committed to git)
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

// Auto-increment version: YYYY.MM.DD.NN
val versionPropsFile = file("version.properties")
val today = LocalDate.now().toString() // "YYYY-MM-DD"
val todayCompact = today.replace("-", "") // "YYYYMMDD"

val versionProps = Properties()
var buildNumber = 1
if (versionPropsFile.exists()) {
    versionProps.load(versionPropsFile.inputStream())
    val lastDate = versionProps.getProperty("lastDate", "")
    buildNumber = if (lastDate == today) {
        versionProps.getProperty("buildNumber", "0").toInt() + 1
    } else {
        1
    }
}
versionProps.setProperty("lastDate", today)
versionProps.setProperty("buildNumber", buildNumber.toString())
versionPropsFile.writer().use { versionProps.store(it, null) }

val buildNN = buildNumber.toString().padStart(2, '0')
val computedVersionCode = "$todayCompact$buildNN".toInt()
val computedVersionName = "${today.replace("-", ".")}.${buildNN}"

android {
    namespace = "com.nabla.notes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nabla.notes"
        minSdk = 26
        targetSdk = 34
        versionCode = computedVersionCode
        versionName = computedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // MSAL client ID from local.properties (no hardcoded secrets)
        buildConfigField(
            "String",
            "MSAL_CLIENT_ID",
            "\"${localProperties.getProperty("msal.clientId", "YOUR_CLIENT_ID_HERE")}\""
        )
    }

    // APK naming: <namespace>-debug.apk for debug, <namespace>.apk for release
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val ns = android.namespace ?: applicationId
            output.outputFileName = if (variant.buildType.name == "debug") "$ns-debug.apk" else "$ns.apk"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.activity.compose)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.coroutines.android)

    // Microsoft MSAL
    implementation(libs.msal)

    // HTTP client
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Markdown rendering
    implementation(libs.markwon.core)
    implementation(libs.markwon.tables)
    implementation(libs.markwon.tasklist)
    implementation(libs.markwon.strikethrough)
    implementation(libs.markwon.linkify)
    implementation(libs.markwon.image)
    implementation(libs.markwon.image.glide)
    implementation(libs.markwon.html)

    // DataStore
    implementation(libs.datastore.preferences)
}
