plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")

    id ("kotlin-android")
    id ("kotlin-kapt")
}

android {
    namespace = "com.example.liststart"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.liststart"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled= true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    viewBinding.isEnabled = true 
}

dependencies {
    implementation("com.google.maps.android:android-maps-utils:2.3.0")//

    implementation ("org.locationtech.proj4j:proj4j:1.1.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")


    // 기존에 있던 라이브러리들 아래에 추가
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.play.services)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.activity.ktx)

    // 테스트 라이브러리
    testImplementation(libs.mockwebserver)
    testImplementation(libs.junit)

    implementation ("androidx.room:room-runtime:2.6.1")
    annotationProcessor ("androidx.room:room-compiler:2.6.1")
    kapt ("androidx.room:room-compiler:2.6.1")// 코틀린 확장 기능

    implementation ("androidx.room:room-ktx:2.4.2")
}
