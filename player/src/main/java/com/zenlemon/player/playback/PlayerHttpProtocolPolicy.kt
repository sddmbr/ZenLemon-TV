package com.zenlemon.player.playback

import com.zenlemon.domain.model.VodHttpProtocolMode

object PlayerHttpProtocolPolicy {

    fun forceHttp1(
        url: String,
        resolvedStreamType: ResolvedStreamType
    ): Boolean {
        // HLS typically performs better with HTTP/2 if available, but some older servers
        // misbehave. We default to allowing HTTP/2 unless there's a known issue.
        return false
    }

    fun forceHttp1(
        resolvedStreamType: ResolvedStreamType,
        vodHttpProtocolMode: VodHttpProtocolMode
    ): Boolean {
        return resolvedStreamType == ResolvedStreamType.PROGRESSIVE &&
            vodHttpProtocolMode == VodHttpProtocolMode.COMPATIBILITY_HTTP1
    }
}
