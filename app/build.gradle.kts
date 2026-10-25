import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val releaseKeystorePath = System.getenv("NOVA_RELEASE_KEYSTORE_PATH")
val releaseStorePassword = System.getenv("NOVA_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("NOVA_RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("NOVA_RELEASE_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.nova.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.omarkhair70.nova"
        minSdk = 26
        targetSdk = 36
        versionCode = 20113
        versionName = "2.1.13"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "PLANTUPIA_QA_API_URL", "\"\"")
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("releaseUpload") {
                storeFile = file(requireNotNull(releaseKeystorePath))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        // Side-by-side QA install, isolated from the Google Play application.
        create("qa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            matchingFallbacks += listOf("debug")
            val localQaUrl = providers.gradleProperty("plantupiaQaApiUrl").orNull
            if (localQaUrl != null) {
                val uri = URI(localQaUrl)
                require(uri.scheme == "http" && uri.host in listOf("10.0.2.2", "127.0.0.1", "localhost") &&
                    uri.path == "/api/v1/" && uri.userInfo == null && uri.query == null && uri.fragment == null) {
                    "Local QA override must be a loopback/emulator HTTP origin ending in /api/v1/"
                }
                buildConfigField("String", "PLANTUPIA_QA_API_URL", "\"$localQaUrl\"")
            }
        }
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("releaseUpload")?.let { signingConfig = it }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

kotlin {
    compilerOptions {
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.firebase.messaging)
    implementation(libs.okhttp)
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-ui:1.10.1")
    implementation("androidx.media3:media3-transformer:1.10.1")
    implementation("androidx.media3:media3-effect:1.10.1")
    implementation("androidx.navigation3:navigation3-runtime:1.1.4")
    implementation("androidx.navigation3:navigation3-ui:1.1.4")
    implementation("androidx.core:core-telecom:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("io.github.webrtc-sdk:android:144.7559.08")
    testImplementation(libs.junit)
    // Exercise the real response parser on the JVM rather than Android's JSON stubs.
    testImplementation("org.json:json:20250517")
    testImplementation("org.mockito:mockito-core:5.23.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
