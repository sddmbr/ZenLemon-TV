package com.zenlemon.app.ui.screens.youtube

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class YouTubeViewModel @Inject constructor() : ViewModel() {
    
    // We start with a default YouTube video ID (this is a standard test video)
    private val _currentVideoId = MutableStateFlow("aqz-KE-bpKQ")
    val currentVideoId: StateFlow<String> = _currentVideoId

    private val _queue = MutableStateFlow(
        listOf(
            YouTubeVideo("aqz-KE-bpKQ", "Big Buck Bunny"),
            YouTubeVideo("dQw4w9WgXcQ", "Never Gonna Give You Up"),
            YouTubeVideo("9bZkp7q19f0", "PSY - GANGNAM STYLE"),
            YouTubeVideo("M7lc1UVf-VE", "YouTube Developers"),
            YouTubeVideo("lA4N_43V3x8", "ZenLemon Introduction")
        )
    )
    val queue: StateFlow<List<YouTubeVideo>> = _queue

    fun playVideo(videoId: String) {
        _currentVideoId.value = videoId
    }

    fun addToQueue(video: YouTubeVideo) {
        if (!_queue.value.any { it.id == video.id }) {
            _queue.value = _queue.value + video
        }
    }

    fun removeFromQueue(videoId: String) {
        _queue.value = _queue.value.filter { it.id != videoId }
    }
}

data class YouTubeVideo(
    val id: String,
    val title: String
)