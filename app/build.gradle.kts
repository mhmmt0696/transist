import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.firebase.crashlytics)
}

val envFile = rootProject.file(".env")
val props = Properties()
if (envFile.exists()) props.load(envFile.inputStream())

android {
    namespace = "com.transist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.transist"
        minSdk = 24
        targetSdk = 35
        versionCode = 23
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "BACKEND_URL", "\"${props.getProperty("BACKEND_URL")}\"")

    }

    buildTypes {
        release {
            // Enables code-related app optimization.
            isMinifyEnabled = false

            // Enables resource shrinking.
            isShrinkResources = false

            ndk {
                // Sembollerin AAB'ye eklenmesini ve Play Console tarafından
                // otomatik olarak algılanmasını sağlar.
                debugSymbolLevel = "SYMBOL_TABLE"
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST" // Çakışan INDEX.LIST dosyasını hariç tut
        }

    }

}

dependencies {

    implementation(libs.speech.to.text)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.play.services.basement)
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.core)
    implementation(libs.grpc.auth)

    implementation(libs.oauth2)
    implementation(libs.gax)


    implementation(libs.billing.client)
    implementation(libs.flexbox)
    implementation(libs.google.identity)
    implementation(libs.googleid)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.google.auth)
    implementation(libs.compose.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.material)
    implementation(libs.fragment)
    implementation(libs.retrofit)
    implementation(libs.retrofit2)
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.firebase.crashlytics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
