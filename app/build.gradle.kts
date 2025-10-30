plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

android {
    namespace = "com.tesis.aplicacionpandax"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tesis.aplicacionpandax"
        minSdk = 26
        targetSdk = 35
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
        compose = true
    }
}

dependencies {

    //Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    //Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    //Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.navigation.runtime.android)
    kapt(libs.room.compiler)

    // ---- Coroutines ----
    implementation(libs.coroutines.android)

    // ---- ViewModel ----
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.compose.material:material-icons-extended:1.7.4")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.compose.material3:material3:1.3.0") // Para MaterialTheme y AlertDialog

    implementation("androidx.compose.animation:animation")

    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.coroutines.test)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(kotlin("test"))


}