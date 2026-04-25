package com.example.player.domain.usecase

import com.example.player.data.db.dao.HiddenVideoDao
import com.example.player.data.db.dao.ImportedVideoDao
import com.example.player.model.SortOption
import com.example.player.model.VideoItem
import com.example.player.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * 封装视频列表查询逻辑。
 * 合并 MediaStore 视频 + 用户导入视频，去重、过滤隐藏项，
 * 并按 [sortOption] 排序后输出。
 */
class GetVideoListUseCase @Inject constructor(
    private val repository:   VideoRepository,
    private val importedDao: ImportedVideoDao,
    private val hiddenDao:  HiddenVideoDao
) {
    /**
     * 返回最终派发给 UI 的视频列表 Flow。
     * 合并了 MediaStore、导入视频、隐藏收藏等数据源。
     */
    operator fun invoke(
        sortOption: SortOption,
        rawVideos: Flow<List<VideoItem>>,
        importedVideos: Flow<List<VideoItem>>,
        hiddenUris: Flow<Set<String>>
    ): Flow<List<VideoItem>> = combine(
        rawVideos,
        importedVideos,
        hiddenUris
    ) { raw, imported, hidden ->
        val mediaStoreUris = raw.map { it.uri.toString() }.toSet()
        val deduped = raw + imported.filter { it.uri.toString() !in mediaStoreUris }
        val visible = deduped.filter { it.uri.toString() !in hidden }
        when (sortOption) {
            SortOption.DATE_DESC     -> visible
            SortOption.DATE_ASC      -> visible.reversed()
            SortOption.NAME_ASC      -> visible.sortedBy { it.displayName.lowercase() }
            SortOption.SIZE_DESC     -> visible.sortedByDescending { it.size }
            SortOption.DURATION_DESC -> visible.sortedByDescending { it.duration }
        }
    }
}
