package com.example.player.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.player.data.db.dao.FolderDao
import com.example.player.data.db.entity.FolderEntity
import com.example.player.data.db.entity.FolderVideoCrossRef
import com.example.player.model.Folder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val folderDao: FolderDao
) : ViewModel() {

    val folders: StateFlow<List<Folder>> = folderDao.observeFoldersWithVideos()
        .map { rows ->
            rows.groupBy { it.folderId }
                .map { (id, list) ->
                    Folder(
                        id = id,
                        name = list.first().folderName,
                        videoUris = list.mapNotNull { it.videoUri }
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            folderDao.insertFolder(FolderEntity(id = UUID.randomUUID().toString(), name = trimmed))
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            folderDao.deleteFolder(folderId)
        }
    }

    fun addVideoToFolder(videoUri: Uri, folderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            folderDao.addCrossRef(FolderVideoCrossRef(folderId, videoUri.toString()))
        }
    }

    fun removeVideoFromFolder(videoUri: Uri, folderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            folderDao.removeCrossRef(folderId, videoUri.toString())
        }
    }
}
