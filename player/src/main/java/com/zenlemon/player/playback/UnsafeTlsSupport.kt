package com.zenlemon.player.playback

import okhttp3.OkHttpClient

fun OkHttpClient.Builder.applyUnsafeTlsBypass(): OkHttpClient.Builder {
    return this
}
