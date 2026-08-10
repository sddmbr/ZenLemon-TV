package com.zenlemon.player

import com.zenlemon.domain.model.StreamType
import com.zenlemon.player.playback.ResolvedStreamType
import com.zenlemon.player.playback.StreamTypeResolver

@Deprecated("Use StreamTypeResolver")
object StreamTypeDetector {
    fun detect(url: String): StreamType {
        return when (StreamTypeResolver.resolve(url = url, isLive = url.contains("/live/", ignoreCase = true))) {
            ResolvedStreamType.HLS -> StreamType.HLS
            ResolvedStreamType.DASH -> StreamType.DASH
            ResolvedStreamType.SMOOTH_STREAMING -> StreamType.SMOOTH_STREAMING
            ResolvedStreamType.MPEG_TS_LIVE -> StreamType.MPEG_TS
            ResolvedStreamType.PROGRESSIVE -> StreamType.PROGRESSIVE
            ResolvedStreamType.RTSP -> StreamType.RTSP
            ResolvedStreamType.UNKNOWN -> StreamType.UNKNOWN
        }
    }
}
