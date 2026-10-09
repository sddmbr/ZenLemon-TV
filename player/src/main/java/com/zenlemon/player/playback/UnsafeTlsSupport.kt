package com.zenlemon.player.playback

import okhttp3.OkHttpClient

/**
 * Previously, this method implemented an unsafe TLS bypass (TrustAll).
 * Due to security vulnerabilities (MitM attacks), it now relies on the
 * default, secure TLS configuration provided by OkHttp.
 */
fun OkHttpClient.Builder.applyUnsafeTlsBypass(): OkHttpClient.Builder {
    // SEC-FIX: Removed insecure TrustAll X509TrustManager and HostnameVerifier.
    // Relying on the default, secure TLS configuration.
    return this
}
