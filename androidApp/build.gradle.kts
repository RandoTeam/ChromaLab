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

val rustGeneratedJniLibsDir = layout.buildDirectory.asFile.get().resolve("generated/rustJniLibs")
val rustBridgeLibrary = rustGeneratedJniLibsDir.resolve("arm64-v8a/libchromalab_cv_core.so")
val buildRustAndroidBridge by tasks.registering(Exec::class) {
    val outputDir = rustGeneratedJniLibsDir
    inputs.file(rootProject.file("rust/Cargo.toml"))
    inputs.file(rootProject.file("rust/Cargo.lock"))
    inputs.dir(rootProject.file("rust/chromalab-cv-core/src"))
    inputs.file(rootProject.file("tools/rust/Build-RustAndroidBridge.ps1"))
    outputs.file(rustBridgeLibrary)

    commandLine(
        "powershell",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        rootProject.file("tools/rust/Build-RustAndroidBridge.ps1").absolutePath,
        "-OutputJniLibs",
        outputDir.absolutePath,
    )
}

android {
    namespace = "com.chromalab.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chromalab.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "0.0.5-beta.6"

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
        getByName("main").jniLibs.srcDir(rustGeneratedJniLibsDir)
        maybeCreate("validation").assets.srcDir(rootProject.file("composeApp/src/androidMain/assets"))
        maybeCreate("validation").jniLibs.srcDir(rustGeneratedJniLibsDir)
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

    ndkVersion = "27.3.13750724"
}

tasks.matching { task ->
    task.name.startsWith("merge") &&
        (task.name.endsWith("JniLibFolders") || task.name.endsWith("NativeLibs"))
}.configureEach {
    dependsOn(buildRustAndroidBridge)
    inputs.file(rustBridgeLibrary)
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
