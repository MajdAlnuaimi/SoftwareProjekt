plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.shre2fix"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.schare2fix"
        minSdk = 29
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
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.image.labeling)
    implementation(libs.common)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.coil.compose)
    implementation(libs.androidx.material)
    implementation(libs.firebase.analytics)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
    implementation("androidx.compose.material:material-icons-extended")
    implementation ("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))

    // Add the dependencies for the App Check libraries
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
        // Firebase Auth
    implementation(libs.firebase.auth)

        // Credential Manager

        // Compose
    implementation(libs.androidx.activity.compose)

    implementation (libs.androidx.credentials) // Credential Manager
    implementation (libs.googleid) // Google ID Token Credential
    implementation (libs.google.firebase.auth.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.play.services.auth)
// Oder aktuelle Version
    implementation(libs.com.google.firebase.firebase.auth.ktx)
    implementation(libs.firebase.database)
    implementation(libs.firebase.appcheck.debug)
// Oder aktuelle Version








    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    //implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    apply(plugin = "com.google.gms.google-services")

}