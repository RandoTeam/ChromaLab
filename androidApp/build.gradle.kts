import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// Load local.properties for signing secrets
val localProps = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) load(localFile.inputStream())
}

android {
    namespace = "com.chromalab.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chromalab.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "0.0.5-beta.5"

        ndk {
            // Target only arm64 (modern phones)
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = localProps.getProperty("RELEASE_STORE_FILE")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS", "")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
            }
        }
    }

    buildTypes {
        debug {
            // Keep the default debug package unchanged for local developer installs.
        }
        create("validation") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".validation"
            versionNameSuffix = "-validation"
            matchingFallbacks += listOf("debug")
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning?.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
    }

    sourceSets {
        maybeCreate("validation").assets.srcDir(rootProject.file("composeApp/src/androidMain/assets"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // NDK build for llama.cpp JNI bridge
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.22.1"
        }
    }

    ndkVersion = "27.2.12479018"
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.activity.compose)
}
