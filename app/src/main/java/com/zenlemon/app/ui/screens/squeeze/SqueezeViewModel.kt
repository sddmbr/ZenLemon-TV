package com.zenlemon.app.ui.screens.squeeze

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SqueezeViewModel @Inject constructor() : ViewModel() {
    
    // We start with a default ZenLemon intro video ID
    private val _currentVideoId = MutableStateFlow("lA4N_43V3x8")
    val currentVideoId: StateFlow<String> = _currentVideoId

    private val _queue = MutableStateFlow(
        listOf(
            SqueezeVideo("lA4N_43V3x8", "ZenLemon Introduction"),
            SqueezeVideo("aqz-KE-bpKQ", "Big Buck Bunny"),
            SqueezeVideo("dQw4w9WgXcQ", "Never Gonna Give You Up"),
            SqueezeVideo("9bZkp7q19f0", "PSY - GANGNAM STYLE"),
            SqueezeVideo("M7lc1UVf-VE", "Developer Quickstart")
        )
    )
    val queue: StateFlow<List<SqueezeVideo>> = _queue

    fun playVideo(videoId: String) {
        _currentVideoId.value = videoId
    }

    fun addToQueue(video: SqueezeVideo) {
        _queue.value = _queue.value + video
    }

    fun removeFromQueue(videoId: String) {
        _queue.value = _queue.value.filter { it.id != videoId }
    }
}

data class SqueezeVideo(
    val id: String,
    val title: String
)
