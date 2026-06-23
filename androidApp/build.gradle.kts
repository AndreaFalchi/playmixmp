import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.falchi.playmixmp"
    compileSdk = project.property("android.compileSdk").toString().toInt()

    defaultConfig {
        applicationId = "com.falchi.playmixmp"
        minSdk = project.property("android.minSdk").toString().toInt()
        targetSdk = project.property("android.targetSdk").toString().toInt()
        versionCode = project.property("android.versionCode").toString().toInt()
        versionName = project.property("android.versionName").toString()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            // Deve essere false per Google Play
            isDebuggable = false
            // DISATTIVA R8/Minificazione per ora
            // Così lo stacktrace nei log sarà leggibile (nomi classi reali)
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.register("incrementVersionCode") {
    val gradlePropertiesFile = layout.projectDirectory.file("../gradle.properties").asFile
    doLast {
        if (gradlePropertiesFile.exists()) {
            val properties = Properties()
            gradlePropertiesFile.inputStream().use { input -> 
                properties.load(input) 
            }
            
            val currentVersionCode = properties.getProperty("android.versionCode")?.toInt() ?: 1
            val newVersionCode = currentVersionCode + 1
            properties.setProperty("android.versionCode", newVersionCode.toString())
            
            gradlePropertiesFile.outputStream().use { output ->
                properties.store(output, "Auto-incremented version code")
            }
            println("VersionCode incremented to: $newVersionCode")
        }
    }
}

// Esegue l'incremento automaticamente prima di generare il bundle o l'APK di release
tasks.configureEach {
    if (name == "generateReleaseBuildConfig" || name == "bundleRelease") {
        dependsOn("incrementVersionCode")
    }
}