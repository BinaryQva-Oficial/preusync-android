/**
 * ============================================================================
 * Proyecto: PreuSync
 * Archivo: build.gradle.kts (Module: app)
 * Descripción: Configuración específica del módulo principal. Declara
 *              dependencias, versiones de SDK y firmas de App.
 * Autor: JiroxDEV
 * ============================================================================
 */
plugins {
    alias(libs.plugins.android.application)
    // ==================== PLUGIN SPOTLESS ====================
    id("com.diffplug.spotless") version "6.19.0"
}

android {
    signingConfigs {
        create("BinaryQva") {
        }
    }
    namespace = "binaryqva.educative.preusync"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "binaryqva.educative.preusync"
        minSdk = 24
        targetSdk = 36
        versionCode = 2755
        versionName = "0.99.115"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionNameSuffix = "-Beta"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildToolsVersion = "36.0.0"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.work.runtime)
    implementation(libs.room.runtime)
    implementation(libs.startup)
    implementation(libs.glide)
    implementation(libs.swiperefreshlayout)
    implementation(libs.splashscreen)
    implementation(libs.commons.codec)
    implementation(libs.prism4j)
    implementation(libs.commonmark)
    implementation(libs.jlatexmath)
    implementation(libs.renderscript.toolkit)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    annotationProcessor(libs.room.compiler)
    annotationProcessor(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    modules {
        module("org.jetbrains:annotations-java5") {
            replacedBy("org.jetbrains:annotations", "Excluyendo duplicados de anotaciones")
        }
    }
}

// ==================== CONFIGURACIÓN DE SPOTLESS ====================
spotless {
    java {
        googleJavaFormat("1.17.0")
        formatAnnotations()
        importOrder("java", "javax", "org", "com", "android", "binaryqva")
        removeUnusedImports()
    }
}