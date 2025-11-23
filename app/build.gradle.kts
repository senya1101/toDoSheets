plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    namespace = "com.example.todolistapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.todolistapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    androidTestImplementation(libs.androidx.espresso.core.v370)
    androidTestImplementation(libs.androidx.espresso.contrib.v370)
    androidTestImplementation(libs.androidx.runner.v152)
    androidTestImplementation(libs.androidx.rules.v150)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.core.v150)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.espresso.contrib)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test.v173)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.mockito.core.v550)
    testImplementation(libs.mockito.kotlin.v510)
    testImplementation(libs.androidx.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlinx.coroutines.test)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.gson)
    implementation("com.google.android.material:material:1.11.0")
    kapt("androidx.room:room-compiler:2.8.3")
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
}

