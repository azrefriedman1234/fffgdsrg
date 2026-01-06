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

        // Telegram API (runtime). Defaults compile fine; set via -PtelegramApiId / -PtelegramApiHash.
        val apiId = (project.findProperty("telegramApiId") as String?) ?: "0"
        val apiHash = (project.findProperty("telegramApiHash") as String?) ?: ""

        buildConfigField("int", "TELEGRAM_API_ID", apiId)
        buildConfigField("String", "TELEGRAM_API_HASH", "\"$apiHash\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    // TDLib AAR (downloaded in CI + Termux to app/libs)
    implementation(files("libs/td-1.8.56.aar"))

    // AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity-ktx:1.9.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Coil
    implementation("io.coil-kt:coil:2.6.0")

    // Media3
    val media3 = "1.9.0"
    implementation("androidx.media3:media3-common:$media3")
    implementation("androidx.media3:media3-transformer:$media3")
    implementation("androidx.media3:media3-effect:$media3")
    implementation("androidx.media3:media3-muxer:$media3")
    implementation("androidx.media3:media3-container:$media3")
    implementation("androidx.media3:media3-extractor:$media3")
    implementation("androidx.media3:media3-decoder:$media3")
    implementation("androidx.media3:media3-datasource:$media3")
    implementation("androidx.media3:media3-database:$media3")
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-ui:$media3")

    // Optional: ML Kit
    implementation("com.google.mlkit:language-id:17.0.6")
    implementation("com.google.mlkit:translate:17.0.3")
}
