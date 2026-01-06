@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pasiflonet.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pasiflonet.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // If you set TELEGRAM_API_ID / TELEGRAM_API_HASH in gradle.properties, they will be exposed here.
        val apiId = (project.findProperty("TELEGRAM_API_ID") as String?) ?: "0"
        val apiHash = (project.findProperty("TELEGRAM_API_HASH") as String?) ?: ""
        buildConfigField("int", "TELEGRAM_API_ID", apiId)
        buildConfigField("String", "TELEGRAM_API_HASH", ""$apiHash"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
        dataBinding = false
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
}

// REQUIRED by user: local AAR in app/libs + flatDir.
repositories {
    flatDir { dirs("libs") }
}

dependencies {
    implementation(files("libs/td-1.8.56.aar"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    // DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Coil
    implementation("io.coil-kt:coil:2.6.0")

    // Media3 (Transformer + playback preview)
    implementation("androidx.media3:media3-transformer:1.9.0")
    implementation("androidx.media3:media3-effect:1.9.0")
    implementation("androidx.media3:media3-common:1.9.0")
    implementation("androidx.media3:media3-muxer:1.9.0")
    implementation("androidx.media3:media3-container:1.9.0")
    implementation("androidx.media3:media3-extractor:1.9.0")
    implementation("androidx.media3:media3-decoder:1.9.0")
    implementation("androidx.media3:media3-datasource:1.9.0")
    implementation("androidx.media3:media3-database:1.9.0")
    implementation("androidx.media3:media3-exoplayer:1.9.0")
    implementation("androidx.media3:media3-ui:1.9.0")

    // Optional (won't be used unless you wire it): ML Kit Language ID + Translate
    implementation("com.google.mlkit:language-id:17.0.6")
    implementation("com.google.mlkit:translate:17.0.3")
}
