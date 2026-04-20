plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.example.player.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 31
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    targetProjectPath = ":app"

    // 说明：
    // androidx.baselineprofile Gradle 插件当前（1.4.0-alpha11）尚不兼容 AGP 9.1；
    // 这里保留 com.android.test 模块承载 Macrobenchmark 用例，
    // 运行方法：
    //   1. 连接 API 33+ 真机（或 Google Play 系统镜像模拟器）；
    //   2. `./gradlew :baselineprofile:connectedCheck`；
    //   3. BaselineProfileGenerator 产物位于
    //      baselineprofile/build/outputs/managed_device_android_test_additional_output/...
    //      或 build/outputs/connected_android_test_additional_output/... 下的 baseline-prof.txt；
    //   4. 手动把该 txt 放到 app/src/main/baseline-prof.txt，profileinstaller 会在安装时编译生效。
    // 待 baselineprofile 插件适配 AGP 9 后，可恢复 `baselineProfile(project(":baselineprofile"))` 自动集成。
}

dependencies {
    implementation("androidx.test.ext:junit:1.3.0")
    implementation("androidx.test.espresso:espresso-core:3.7.0")
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
