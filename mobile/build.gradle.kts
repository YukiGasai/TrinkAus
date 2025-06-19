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
        minSdk = 28
        targetSdk = 35
        versionCode = 15
        versionName = "1.15"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    packaging {
        resources.excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        resources.excludes.add("META-INF/INDEX.LIST")
        resources.excludes.add("META-INF/io.netty.versions.properties")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.connect.client)

    // --- COMPOSE ---
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.runtime)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // --- GLANCE ---
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // --- WEAR OS & GOOGLE PLAY SERVICES ---
    implementation(libs.play.services.wearable)

    // --- 3RD PARTY LIBS ---
    implementation(libs.konfetti.compose)
    implementation(libs.compose.charts)
    implementation(libs.icons.lucide)

    // --- MODULES ---
    implementation(project(":shared")) // Assuming you have a ":shared" module

    // --- TESTING ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    implementation("io.ktor:ktor-server-core-jvm:3.1.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.1.3")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.11")
}
