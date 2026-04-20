package com.example.player.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 冷启动耗时基准。
 *
 * 运行：`./gradlew :baselineprofile:connectedBenchmarkAndroidTest`
 *
 * 会产出三组数据：
 *  - CompilationMode.None  : 纯解释执行（最坏情况）
 *  - CompilationMode.Partial(BaselineProfile) : 含 Baseline Profile
 *  - CompilationMode.Full  : 全编译（最好情况，仅供对标）
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() = startup(CompilationMode.None())

    @Test
    fun startupPartialWithBaselineProfile() = startup(
        CompilationMode.Partial(baselineProfileMode = androidx.benchmark.macro.BaselineProfileMode.Require)
    )

    @Test
    fun startupFullyCompiled() = startup(CompilationMode.Full())

    private fun startup(mode: CompilationMode) = rule.measureRepeated(
        packageName = "com.example.player",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = mode,
        iterations = 5,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
    }
}
