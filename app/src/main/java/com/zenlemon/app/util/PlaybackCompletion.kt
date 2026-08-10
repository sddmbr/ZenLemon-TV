package com.zenlemon.app.util

import com.zenlemon.domain.util.isPlaybackComplete as domainIsPlaybackComplete

fun isPlaybackComplete(
    progressMs: Long,
    totalDurationMs: Long,
    threshold: Float = com.zenlemon.domain.util.DEFAULT_PLAYBACK_COMPLETION_THRESHOLD
): Boolean = domainIsPlaybackComplete(progressMs, totalDurationMs, threshold)
