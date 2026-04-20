package com.example.player.viewmodel

import com.example.player.model.VideoItem

/**
 * 主页 UI 状态机（单向数据流 MVI 模式）
 *
 * - Loading : MediaStore 正在扫描且尚无任何视频（首次启动）
 * - Success : 列表中至少有一条视频记录
 * - Empty   : 扫描完成但媒体库为空
 * - Error   : 扫描失败（权限缺失 / IO 异常 / 其他），附带 retry 选项
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class  Success(val videos: List<VideoItem>) : HomeUiState
    data object Empty   : HomeUiState
    data class  Error(val reason: Reason, val message: String? = null) : HomeUiState {
        /** 错误分类；供 UI 做精细化引导（如跳权限设置页）。 */
        enum class Reason { PERMISSION_DENIED, IO, UNKNOWN }
    }
}
