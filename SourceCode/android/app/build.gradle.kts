import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.maps.secrets)
}
secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}
// TV2 - Thịnh: tách endpoint theo build type, giữ plugin/version/dependency gốc của nhóm.
val debugApiUrl = providers.gradleProperty("CITYGUARD_DEBUG_BASE_URL").orElse("http://10.0.2.2:8080/")
val releaseApiUrl = providers.gradleProperty("CITYGUARD_RELEASE_BASE_URL").orElse("")
fun quotedUrl(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
// TV2: chủ động chặn release thiếu HTTPS hoặc dùng localhost/LAN; không tự điền URL staging giả.
val verifyReleaseApiUrl by tasks.registering {
    inputs.property("baseUrl", releaseApiUrl)
    doLast {
        val url = inputs.properties.getValue("baseUrl").toString()
        val uri = runCatching { URI(url) }.getOrNull()
        val host = uri?.host.orEmpty()
        val local = host.equals("localhost", true) || host.endsWith(".localhost", true) ||
            host.endsWith(".local", true) || host == "::1" || host == "[::1]" ||
            host.startsWith("127.") || host.startsWith("10.") || host.startsWith("192.168.") ||
            Regex("172\\.(1[6-9]|2[0-9]|3[01])\\..*").matches(host)
        check(uri?.scheme == "https" && host.isNotBlank() && !local &&
            uri?.userInfo == null && uri?.query == null && uri?.fragment == null &&
            url.endsWith("/")) {
            "Release cần CITYGUARD_RELEASE_BASE_URL là HTTPS công khai, kết thúc bằng / và không dùng localhost/LAN."
        }
    }
}
tasks.configureEach {
    if (name == "preReleaseBuild") dependsOn(verifyReleaseApiUrl)
}
android {
    namespace = "com.hoangpha.cityguard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.cityguard.app"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["usesCleartextTraffic"] = "true"
    }

    buildTypes {
        // TV2: debug dùng emulator/local; release chỉ nhận URL HTTPS được nhóm cấu hình.
        debug {
            buildConfigField("String", "BASE_URL", quotedUrl(debugApiUrl.get()))
        }
        release {
            buildConfigField("String", "BASE_URL", quotedUrl(releaseApiUrl.get()))
            optimization {
                enable = false
            }
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.google.maps.compose)
    implementation(libs.google.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.test:core-ktx:1.5.0")

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.7")
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

}

// Robolectric 4.11.1 của project chạy test bằng Java 21, tách với Gradle daemon.
tasks.withType<Test>().configureEach {
    // TV2: in tên từng test để nhóm xác định đúng ca lỗi khi build/test trên máy hoặc CI.
    testLogging { events("started", "passed", "failed", "skipped") }
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}
