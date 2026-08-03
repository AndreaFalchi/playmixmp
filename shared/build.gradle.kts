import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    android {
       namespace = "com.falchi.playmixmp.shared"
       compileSdk = project.property("android.compileSdk").toString().toInt()
       minSdk = project.property("android.minSdk").toString().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/kotlin"))
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.media3.exoplayer)
            implementation(libs.documentfile)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

val generateBuildInfo = tasks.register("generateBuildInfo") {
    val outputFile = layout.buildDirectory.file("generated/kotlin/BuildInfo.kt")
    
    val agpVersion = libs.versions.agp.get()
    val kotlinVersion = libs.versions.kotlin.get()
    val composeVersion = libs.versions.composeMultiplatform.get()
    val material3Version = libs.versions.material3.get()
    
    val compileSdk = project.property("android.compileSdk").toString()
    val minSdk = project.property("android.minSdk").toString()
    val targetSdk = project.property("android.targetSdk").toString()
    val appVersionName = project.property("android.versionName").toString()
    val appVersionCode = project.property("android.versionCode").toString()

    inputs.property("agpVersion", agpVersion)
    inputs.property("kotlinVersion", kotlinVersion)
    inputs.property("composeVersion", composeVersion)
    inputs.property("material3Version", material3Version)
    inputs.property("compileSdk", compileSdk)
    inputs.property("minSdk", minSdk)
    inputs.property("targetSdk", targetSdk)
    inputs.property("appVersionName", appVersionName)
    inputs.property("appVersionCode", appVersionCode)
    
    outputs.file(outputFile)

    doLast {
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                package com.falchi.playmixmp
                
                object BuildInfo {
                    const val AGP_VERSION = "$agpVersion"
                    const val KOTLIN_VERSION = "$kotlinVersion"
                    const val COMPOSE_VERSION = "$composeVersion"
                    const val MATERIAL3_VERSION = "$material3Version"
                    const val COMPILE_SDK = "$compileSdk"
                    const val MIN_SDK = "$minSdk"
                    const val TARGET_SDK = "$targetSdk"
                    const val APP_VERSION_NAME = "$appVersionName"
                    const val APP_VERSION_CODE = "$appVersionCode"
                }
                """.trimIndent()
            )
        }
    }
}

tasks.configureEach {
    if (name.contains("compileKotlin") || name.contains("compileJava")) {
        dependsOn(generateBuildInfo)
    }
}
