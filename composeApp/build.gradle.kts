import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    androidLibrary {
        namespace = "com.chromalab.shared"
        compileSdk = 35
        minSdk = 26
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)

            // Navigation
            implementation(libs.navigation.compose)

            // Lifecycle
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            // Coroutines
            implementation(libs.coroutines.core)

            // Serialization
            implementation(libs.serialization.json)

            // DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            // Image loading
            implementation(libs.coil.compose)
            implementation(libs.coil.network)
            implementation(libs.richtext.commonmark)
            implementation(libs.richtext.ui.material3)

            // Room
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            // CameraX
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.view)

            // EXIF
            implementation(libs.exifinterface)

            // Coroutines Android
            implementation(libs.coroutines.android)

            // ML Kit Text Recognition (OCR)
            implementation(libs.mlkit.text)
            // ML Kit Document Scanner (crop + deskew + shadow removal)
            implementation(libs.mlkit.document.scanner)

            // LiteRT-LM — on-device LLM inference (Gemma 4)
            implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.openpnp.opencv)
            }
        }
    }
}

// Room KSP — process annotations for each target
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
}

compose.desktop {
    application {
        mainClass = "com.chromalab.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ChromaLab"
            packageVersion = "1.0.0"
        }
    }
}
