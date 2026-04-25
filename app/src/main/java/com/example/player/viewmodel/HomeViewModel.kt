package com.example.player.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.player.data.datastore.SettingsDataStore
import com.example.player.data.db.dao.FavoriteDao
import com.example.player.data.db.dao.HiddenVideoDao
import com.example.player.data.db.dao.ImportedVideoDao
import com.example.player.data.db.dao.PlaybackPositionDao
import com.example.player.data.db.entity.FavoriteEntity
import com.example.player.data.db.entity.HiddenVideoEntity
import com.example.player.data.db.entity.ImportedVideoEntity
import com.example.player.domain.usecase.ClearFavoritesUseCase
import com.example.player.domain.usecase.ClearHistoryUseCase
import com.example.player.domain.usecase.ClearThumbnailCacheUseCase
import com.example.player.domain.usecase.GetThumbnailCacheSizeUseCase
import com.example.player.model.SortOption
import com.example.player.model.VideoItem
import com.example.player.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val repository:    VideoRepository,
    private val favoriteDao:   FavoriteDao,
    private val hiddenDao:     HiddenVideoDao,
    private val importedDao:   ImportedVideoDao,
    private val positionDao:   PlaybackPositionDao,
    private val settings:      SettingsDataStore,
    private val clearHistoryUseCase: ClearHistoryUseCase,
    private val clearFavoritesUseCase: ClearFavoritesUseCase,
    private val clearThumbnailCacheUseCase: ClearThumbnailCacheUseCase,
    private val getThumbnailCacheSizeUseCase: GetThumbnailCacheSizeUseCase
) : AndroidViewModel(application) {

    // ── 原始响应式数据源 ────────────────────────────────────────────────────

    /** MediaStore 本地视频（ContentObserver 推送） */
    private val _rawVideos = repository.observeLocalVideos()
        .catch { _loadError.value = HomeUiState.Error(HomeUiState.Error.Reason.IO, it.localizedMessage); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 用户手动导入的视频 */
    private val _importedVideos: StateFlow<List<VideoItem>> = importedDao.observeAll()
        .map { list -> list.map { it.toVideoItem() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // MediaStore 扫描是否正在进行（首次订阅 + 手动 retry 时短暂为 true）
    private val _isLoadingMediaStore = MutableStateFlow(true).also { flag ->
        // 首次收到 rawVideos 后清除 loading 标记
        viewModelScope.launch {
            _rawVideos.first()   // 等待首帧
            flag.value = false
        }
    }

    // 扫描错误（null 表示无错误）
    private val _loadError = MutableStateFlow<HomeUiState.Error?>(null)

    // ── 公开的持久化流 ──────────────────────────────────────────────────────

    /** 收藏集合（uri 字符串） */
    val favorites: StateFlow<Set<String>> = favoriteDao.observeUris()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** 已从列表隐藏的视频（uri 字符串） */
    val hiddenVideos: StateFlow<Set<String>> = hiddenDao.observeUris()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** uri.toString() → 已保存的播放位置（毫秒） */
    val savedPositions: StateFlow<Map<String, Long>> = positionDao.observeAll()
        .map { list -> list.associate { it.uri to it.positionMs } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** 当前排序方式（来自 DataStore） */
    val currentSort: StateFlow<SortOption> = settings.sortFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortOption.DATE_DESC)

    // ── 最终派发给 UI 的视频列表 ────────────────────────────────────────────

    /**
     * rawVideos + imported（去重）- hidden → 按 currentSort 排序。
     * 任一上游变动自动触发重算。
     */
    val videos: StateFlow<List<VideoItem>> = combine(
        _rawVideos, _importedVideos, hiddenVideos, currentSort
    ) { raw, imported, hidden, sort ->
        val mediaStoreUris = raw.map { it.uri.toString() }.toSet()
        val deduped = raw + imported.filter { it.uri.toString() !in mediaStoreUris }
        val visible = deduped.filter { it.uri.toString() !in hidden }
        when (sort) {
            SortOption.DATE_DESC     -> visible
            SortOption.DATE_ASC      -> visible.reversed()
            SortOption.NAME_ASC      -> visible.sortedBy { it.displayName.lowercase() }
            SortOption.SIZE_DESC     -> visible.sortedByDescending { it.size }
            SortOption.DURATION_DESC -> visible.sortedByDescending { it.duration }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * 主页 UI 状态（MVI）：
     *  Loading → 首帧未到达且列表为空
     *  Error   → 扫描失败且列表为空（含重试）
     *  Success → 列表中至少有一条视频
     *  Empty   → 扫描完成但无任何视频
     */
    val uiState: StateFlow<HomeUiState> = combine(
        _isLoadingMediaStore, videos, _loadError
    ) { isLoading, list, error ->
        when {
            isLoading && list.isEmpty()       -> HomeUiState.Loading
            error != null && list.isEmpty()   -> error
            list.isEmpty()                    -> HomeUiState.Empty
            else                              -> HomeUiState.Success(list)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState.Loading)

    init {
        // 启动后台缩略图预生成（KEEP 策略：已排队则不重复排队）
        enqueueThumbnailPrewarm()
    }

    private fun enqueueThumbnailPrewarm() {
        val ctx = getApplication<Application>()
        val constraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val request = androidx.work.OneTimeWorkRequestBuilder<com.example.player.work.ThumbnailWorker>()
            .setInitialDelay(5, java.util.concurrent.TimeUnit.SECONDS)
            .setConstraints(constraints)
            .build()
        androidx.work.WorkManager.getInstance(ctx).enqueueUniqueWork(
            com.example.player.work.ThumbnailWorker.UNIQUE_NAME,
            androidx.work.ExistingWorkPolicy.KEEP,
            request
        )
    }

    // ── 排序 ────────────────────────────────────────────────────────────────

    fun setSortOption(option: SortOption) {
        viewModelScope.launch { settings.setSort(option) }
    }

    // ── 手动导入视频 ─────────────────────────────────────────────────────────

    fun importVideos(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = _importedVideos.value.map { it.uri.toString() }.toSet()
            for (uri in uris) {
                if (uri.toString() in existing) continue
                // 申请持久化 URI 权限，确保重启后仍可访问
                runCatching {
                    getApplication<Application>().contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val v = repository.getVideoMetadata(uri)
                importedDao.insert(
                    ImportedVideoEntity(
                        uri         = v.uri.toString(),
                        displayName = v.displayName,
                        duration    = v.duration,
                        size        = v.size,
                        isLandscape = v.isLandscape
                    )
                )
            }
        }
    }

    // ── 隐藏视频 ─────────────────────────────────────────────────────────────

    fun hideVideo(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            hiddenDao.add(HiddenVideoEntity(uri.toString()))
        }
    }

    // ── 收藏 ────────────────────────────────────────────────────────────────

    fun toggleFavorite(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val key = uri.toString()
            if (favoriteDao.isFavorite(key)) favoriteDao.remove(key)
            else favoriteDao.add(FavoriteEntity(key))
        }
    }

    // ── 播放历史 ────────────────────────────────────────────────────────────

    fun clearPlayHistory() {
        viewModelScope.launch(Dispatchers.IO) { clearHistoryUseCase() }
    }

    fun clearFavorites() {
        viewModelScope.launch(Dispatchers.IO) { clearFavoritesUseCase() }
    }

    fun clearThumbnailCache(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = clearThumbnailCacheUseCase()
            withContext(Dispatchers.Main) { onResult(ok) }
        }
    }

    fun getThumbnailCacheSize(): Long = getThumbnailCacheSizeUseCase()

    // ── 兼容旧接口（UI 层仍在调用） ──────────────────────────────────────────

    /** 供从播放器返回时调用；因为 positions 已是 Flow，实际不再需要手动刷新。 */
    fun refreshPositions() { /* no-op — Flow 自动推送 */ }

    /** 触发一次强制重新订阅（通常只有权限变化后调用）。 */
    fun loadVideos() {
        _loadError.value = null
        // 订阅是 eager 的，这里仅清错误；真正重载会由 Flow 底层驱动。
    }

    fun retryLoad() = loadVideos()

    fun getSavedPosition(uri: Uri): Long = savedPositions.value[uri.toString()] ?: 0L

    /** 供 MainActivity 在导航前同步查询视频方向，命中内存缓存（< 1ms） */
    fun getIsLandscape(uri: Uri): Boolean {
        val key = uri.toString()
        return (_rawVideos.value + _importedVideos.value)
            .firstOrNull { it.uri.toString() == key }
            ?.isLandscape ?: false
    }

    // ── 映射工具 ────────────────────────────────────────────────────────────

    private fun ImportedVideoEntity.toVideoItem() = VideoItem(
        uri         = Uri.parse(uri),
        displayName = displayName,
        duration    = duration,
        size        = size,
        isLandscape = isLandscape
    )
}
