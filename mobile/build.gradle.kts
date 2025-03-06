plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.yukigasai.trinkaus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yukigasai.trinkaus"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    android {
        buildFeatures {
            compose = true
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.1.0"
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.wearable)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.activity.compose)

    implementation("androidx.health.connect:connect-client:1.1.0-alpha12")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.material3:material3-android:1.3.1")
    implementation("androidx.compose.runtime:runtime:1.7.8")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.compose.ui:ui-graphics:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")

    implementation("com.google.android.gms:play-services-wearable:19.0.0")




    wearApp(project(":wear"))

    implementation("androidx.work:work-runtime-ktx:2.7.1")
}