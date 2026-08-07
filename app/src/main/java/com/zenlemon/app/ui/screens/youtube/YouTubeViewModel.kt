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

    fun playVideo(videoId: String) {
        _currentVideoId.value = videoId
    }
}