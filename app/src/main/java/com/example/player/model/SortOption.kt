package com.example.player.model

enum class SortOption(val label: String) {
    DATE_DESC("最新修改"),
    DATE_ASC("最早修改"),
    NAME_ASC("名称 A-Z"),
    SIZE_DESC("文件最大"),
    DURATION_DESC("时长最长")
}
