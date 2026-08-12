package com.zenlemon.app.ui.screens.squeeze

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenlemon.app.di.AuxiliaryPlayerEngine
import com.zenlemon.app.plugins.ZenLemonPluginManager
import com.zenlemon.domain.model.Result
import com.zenlemon.domain.model.StreamInfo
import com.zenlemon.player.PlayerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class SqueezeViewModel @Inject constructor(
    private val pluginManager: ZenLemonPluginManager,
    @AuxiliaryPlayerEngine private val playerEngineProvider: Provider<PlayerEngine>
) : ViewModel() {

    private val _playerEngine = MutableStateFlow<PlayerEngine?>(null)
    val playerEngine: StateFlow<PlayerEngine?> = _playerEngine.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
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

    init {
        // Prepare the first video on init
        playVideo("lA4N_43V3x8")
    }

    fun playVideo(videoId: String) {
        _currentVideoId.value = videoId
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val youtubeUrl = "https://www.youtube.com/watch?v=$videoId"
            val result = pluginManager.preparePlaybackStreamInfo(StreamInfo(url = youtubeUrl))

            when (result) {
                is Result.Success -> {
                    val engine = _playerEngine.value ?: playerEngineProvider.get().also {
                        _playerEngine.value = it
                    }
                    engine.prepare(result.data)
                    engine.play()
                    _isLoading.value = false
                }
                is Result.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                Result.Loading -> {
                    // Handled by _isLoading.value = true above
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _playerEngine.value?.let {
            it.stop()
            it.release()
        }
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
