package com.example.player.domain.usecase

import com.example.player.model.VideoItem
import com.example.player.util.PinyinSearch

/**
 * 封装视频搜索逻辑。
 * 支持三种命中方式（取 OR）：
 *  1. 原文 contains（忽略大小写）
 *  2. 全拼 contains —— "春节晚会" → "chunjiewanhui"
 *  3. 拼音首字母 contains —— "春节晚会" → "cjwh"
 *
 * query 含中文时只走原文匹配；纯 ASCII query 额外触发拼音匹配。
 */
class SearchVideosUseCase {

    /**
     * 返回 [videos] 中命中的视频列表（保持原顺序）。
     * 若 [query] 为空则返回空列表。
     */
    operator fun invoke(videos: List<VideoItem>, query: String): List<VideoItem> {
        if (query.isBlank()) return emptyList()
        return videos.filter { PinyinSearch.matches(it.displayName, query) }
    }
}
