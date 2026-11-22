plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Where the app looks for the online catalog. Change OWNER/REPO if you fork the project.
// "HEAD" always resolves to the repository's default branch.
val catalogBaseUrl = providers.gradleProperty("catalogBaseUrl")
    .getOrElse("https://raw.githubusercontent.com/NoVanity99/Wallpapers/HEAD/catalog/")

// CI passes the run number so every build can be installed over the previous one.
val ciVersionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
val ciVersionName = System.getenv("VERSION_NAME") ?: "1.0.0"

android {
    namespace = "io.github.novanity99.wallpaperselector"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "io.github.novanity99.wallpaperselector"
        minSdk = 26
        targetSdk = 37
        versionCode = ciVersionCode
        versionName = ciVersionName
        buildConfigField("String", "CATALOG_BASE_URL", "\"$catalogBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            // Use your own private key by setting these environment variables
            // (see README, "Signing"). Otherwise the shared sideload key is used.
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "${rootDir}/keystore/sideload.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "wallpaperselector"
            keyAlias = System.getenv("KEY_ALIAS") ?: "wallpaperselector"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "wallpaperselector"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
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

    packaging {
        resources.excludes += setOf("META-INF/{AL2.0,LGPL2.1}", "META-INF/*.version", "kotlin/**", "DebugProbesKt.bin")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.11.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.1")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
