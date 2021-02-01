plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    compileSdkVersion(30)
    buildToolsVersion("30.0.3")

    defaultConfig {
        applicationId = "com.devfutech.chatqiscus"
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "APP_ID_SAMPLE", "\"sdksample\"")
        buildConfigField("String", "APP_ID", "\"alok-f6qsziirbschjhil\"")
        buildConfigField("String", "BASE_URL_API", "\".qiscus.com/api/v2.1/rest/\"")
        buildConfigField("String", "SECRET_KEY", "\"a925a5e8fbda258e5acffebcf5785b03\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.21")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("com.google.android.material:material:1.2.1")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.3")

    //Livedata
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")

    //Hilt
    implementation("com.google.dagger:hilt-android:2.31.2-alpha")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.2.0")
    kapt("com.google.dagger:hilt-android-compiler:2.31.2-alpha")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")
    kapt("androidx.hilt:hilt-compiler:1.0.0-alpha03")

    //MultiDex Application
    implementation("androidx.multidex:multidex:2.0.1")

    //Qiscus SDK
    implementation("com.qiscus.sdk:chat-core:1.3.26")

    //Emoji
    implementation("com.vanniktech:emoji-one:0.5.0")

    //Coil
    implementation("io.coil-kt:coil:1.1.1")

    //Exifinterface
    implementation("androidx.exifinterface:exifinterface:1.3.2")

    //Browser
    implementation("androidx.browser:browser:1.3.0")

    testImplementation("junit:junit:4.13.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}

kapt {
    correctErrorTypes = true
}