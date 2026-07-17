plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

// 读取 keystore.properties（key=value 格式），解析为 Map
fun readProps(file: java.io.File): Map<String, String> {
    val map = mutableMapOf<String, String>()
    file.reader().forEachLine { line ->
        val trimmed = line.trim()
        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
            val eq = trimmed.indexOf('=')
            if (eq > 0) {
                map[trimmed.substring(0, eq).trim()] = trimmed.substring(eq + 1).trim()
            }
        }
    }
    return map
}

android {
    namespace = "com.alosir.task"
    compileSdk = 34

    // 签名：有 keystore.properties 则用自己的签名，没有则用 debug 签名
    // 开源者：复制 keystore.properties.sample → keystore.properties，填入自己的签名信息即可
    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = if (keystorePropsFile.exists()) readProps(keystorePropsFile) else emptyMap()

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps["RELEASE_STORE_FILE"]!!)
                storePassword = keystoreProps["RELEASE_STORE_PASSWORD"]
                keyAlias = keystoreProps["RELEASE_KEY_ALIAS"]
                keyPassword = keystoreProps["RELEASE_KEY_PASSWORD"]
            }
        }
    }

    defaultConfig {
        applicationId = "com.alosir.task"
        minSdk = 21
        targetSdk = 34
        versionCode = 41
        versionName = "1.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    
    implementation(libs.androidx.workmanager.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson)
    implementation(libs.coil)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.shortcutbadger)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

kapt {
    correctErrorTypes = true
}
