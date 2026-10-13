plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android { namespace = "com.sugartrack"; compileSdk = 36
    buildFeatures { compose = true }
    defaultConfig { applicationId = "com.akilnova.glucotime"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "1.0.0" }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("GLUCOTIME_KEYSTORE_PATH")
            val keystorePassword = System.getenv("GLUCOTIME_KEYSTORE_PASSWORD")
            val keyPassword = System.getenv("GLUCOTIME_KEY_PASSWORD")
            if (!keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = System.getenv("GLUCOTIME_KEY_ALIAS") ?: "glucotime_upload"
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}
