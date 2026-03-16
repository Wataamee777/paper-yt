plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.chaquo.python")
}

android {
    namespace = "com.paperyt"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.paperyt"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "Alpha 0.0.3.93"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters("armeabi-v7a", "arm64-v8a", "x86_64")
        }
        flavorDimensions += "pyVersion"
        productFlavors {
            create("py310") { dimension = "pyVersion" }
            create("py311") { dimension = "pyVersion" }
    }
    }

chaquopy {
    productFlavors {
        getByName("py310") { version = "3.10" }
        getByName("py311") { version = "3.11" }
    }
    defaultConfig {
        pip {
            install("yt-dlp")
        }
    }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH")
            if (keystorePath.isPresent) {
                signingConfig = signingConfigs.create("releaseFromEnv") {
                    storeFile = file(keystorePath.get())
                    storePassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
                    keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
                    keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
                }
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "**/libc++_shared.so"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("com.google.android.material:material:1.12.0")
    // Note: FFmpeg Kitは環境によってChaquoと競合する場合があるため、ビルドエラーが出たら相談してください
    implementation("com.github.arthurhub:ffmpeg-kit:ffmpeg-kit-full:6.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
