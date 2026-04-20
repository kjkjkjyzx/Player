package com.example.player.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline Profile 生成器。
 *
 * 运行方法：
 *   1. 插一台 API 33+ 真机（或 Google Play 系统镜像模拟器）；
 *   2. `./gradlew :app:generateBaselineProfile`；
 *   3. 完成后 Profile 会写入 `app/src/main/baselineProfiles/baseline-prof.txt`，随下一次 APK 一并发布。
 *
 * 覆盖的用户旅程：
 *  - 冷启动到首页；
 *  - 首页 Lazy 列表滚动若干帧；
 *  - 进入 PlayerScreen（若列表非空）并退出。
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()

        // 等待首页就绪
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 5_000)

        // 滚动首页列表，让 Compose 关键路径得到记录
        repeat(3) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
                         device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle(500)
        }
    }

    companion object {
        private const val PACKAGE_NAME = "com.example.player"
    }
}
