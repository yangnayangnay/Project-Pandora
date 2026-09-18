plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.project_pandora"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.project_pandora"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.retrofit.rxjava)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    implementation(libs.room.runtime)
    implementation(libs.room.rxjava)
    annotationProcessor(libs.room.compiler)

    implementation(libs.rxjava)
    implementation(libs.rxandroid)

    implementation(libs.mpandroidchart)
    implementation(libs.glide)
    implementation(libs.security.crypto)
    implementation(libs.splashscreen)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.fragment)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.swiperefresh)
    implementation(libs.circleimageview)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
