import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlinSerializaitons)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
}
room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.conkeep"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.conkeep"
        minSdk = 28
        targetSdk = 36
        versionCode = 14
        versionName = "0.0.12"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            type = "String",
            "WEB_CLIENT_ID",
            "\"${gradleLocalProperties(rootDir, providers).getProperty("WEB_CLIENT_ID")}\"",
        )
        buildConfigField(
            type = "String",
            "BASE_URL",
            "\"${gradleLocalProperties(rootDir, providers).getProperty("BASE_URL")}\"",
        )
        buildConfigField(
            type = "String",
            "SUPABASE_URL",
            "\"${gradleLocalProperties(rootDir, providers).getProperty("SUPABASE_URL")}\"",
        )
        buildConfigField(
            type = "String",
            "SUPABASE_ANON_KEY",
            "\"${gradleLocalProperties(rootDir, providers).getProperty("SUPABASE_ANON_KEY")}\"",
        )
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Paging3
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Supabase
    implementation(platform(libs.bom))
    implementation(libs.supabase.postgrest.kt)
    implementation(libs.supabase.auth.kt)
    implementation(libs.realtime.kt)

    // Google Sign-In
    implementation(libs.play.services.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.hilt.work)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.room.compiler)

    // Navigation3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Ktor
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.ktor3)
    implementation(libs.zoomable.image.coil3)

    // Google
    implementation(libs.barcode.scanning)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    // Kotlinx
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.datetime)

    // Shimmer
    implementation(libs.compose.shimmer)

    // Barcode
    implementation(libs.zxing.core)

    implementation(libs.datetime.wheel.picker)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.register<Zip>("zipNativeDebugSymbols") {
    from("build/intermediates/merged_native_libs/release/out/lib")
    archiveFileName.set("native-debug-symbols.zip")
    destinationDirectory.set(file("build/outputs/bundle/release"))
}

tasks.matching { it.name == "bundleRelease" }.configureEach {
    finalizedBy("zipNativeDebugSymbols")
}
