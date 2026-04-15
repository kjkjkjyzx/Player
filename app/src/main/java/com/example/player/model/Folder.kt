package com.example.player.model

data class Folder(
    val id: String,
    val name: String,
    val videoUris: List<String> = emptyList()
)
