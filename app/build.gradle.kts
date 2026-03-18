import java.util.Properties

plugins {
    id("com.android.application") version "8.13.2" // AGP recente compatível
    id("org.jetbrains.kotlin.android") version "2.0.21" // Kotlin estável
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" // Para Compose
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
}

android {
    val localProps = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }
    fun prop(name: String): String? =
        (localProps.getProperty(name) ?: System.getenv(name))?.takeIf { it.isNotBlank() }

    val releaseStoreFile = prop("RELEASE_STORE_FILE")
    val releaseStorePassword = prop("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = prop("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = prop("RELEASE_KEY_PASSWORD")
    val googleWebClientId = (prop("GOOGLE_WEB_CLIENT_ID") ?: "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    val supabaseUrl = (prop("SUPABASE_URL") ?: "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    val supabaseAnonKey = (prop("SUPABASE_ANON_KEY") ?: "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

    namespace = "com.financeiro.financeiro"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.joaod.financeiro.v13"
        minSdk = 33
        targetSdk = 36
        versionCode = 9
        versionName = "1.8"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile != null) {
                storeFile = rootProject.file(releaseStoreFile)
            }
            if (releaseStorePassword != null) storePassword = releaseStorePassword
            if (releaseKeyAlias != null) keyAlias = releaseKeyAlias
            if (releaseKeyPassword != null) keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.credentials:credentials:1.6.0-rc02")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0-rc02")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
