plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Removed kotlin.compose plugin and replaced with direct compiler configuration
    id("com.google.devtools.ksp") version "1.9.22-1.0.16" // Downgraded KSP version for better compatibility
}

android {
    namespace = "com.purrytify.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.purrytify.mobile"
        minSdk = 29
        targetSdk = 34
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8" // Compatible with Kotlin 1.9.22
    }
}

dependencies {
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.palette.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
//    implementation(libs.navigation.compose)

    // Room Database dependencies
    val roomVersion = "2.6.0"
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.glide)
    implementation(libs.coil.compose)
    implementation("androidx.media:media:1.6.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    
    // OpenStreetMap for embedded map without API keys
    implementation("org.osmdroid:osmdroid-android:6.1.17")
    implementation("org.osmdroid:osmdroid-wms:6.1.17")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.17")
}