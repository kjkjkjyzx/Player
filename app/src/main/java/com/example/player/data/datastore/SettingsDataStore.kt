package com.example.player.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.player.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 全局 Preferences DataStore — 文件名 `settings.preferences_pb`。通过扩展属性单例化。 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 轻量级设置仓库。
 * 目前只承载用户选择的 [SortOption]；后续加主题、默认音量等都在此扩展。
 */
class SettingsDataStore(private val context: Context) {

    private val sortKey = stringPreferencesKey("sort_option")

    /** 发射用户当前排序；读失败或未设置返回默认 [SortOption.DATE_DESC]。 */
    val sortFlow: Flow<SortOption> = context.settingsDataStore.data.map { prefs ->
        prefs[sortKey]
            ?.let { name -> runCatching { SortOption.valueOf(name) }.getOrNull() }
            ?: SortOption.DATE_DESC
    }

    suspend fun setSort(option: SortOption) {
        context.settingsDataStore.edit { it[sortKey] = option.name }
    }
}
