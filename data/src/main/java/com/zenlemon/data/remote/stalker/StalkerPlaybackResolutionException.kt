package com.zenlemon.data.remote.stalker

import com.zenlemon.domain.model.StalkerBootstrapRecipe
import com.zenlemon.domain.model.StalkerCookieMode
import com.zenlemon.domain.model.StalkerEndpointPreference
import com.zenlemon.domain.model.StalkerMagPreset
import com.zenlemon.domain.model.StalkerPlaybackBackendHint
import com.zenlemon.domain.model.StalkerPortalFingerprint
import java.io.IOException

class StalkerPlaybackResolutionException(
    message: String,
    cause: Throwable? = null,
    val streamKind: StalkerStreamKind = StalkerStreamKind.LIVE,
    val portalFingerprint: StalkerPortalFingerprint? = null,
    val magPreset: StalkerMagPreset? = null,
    val bootstrapRecipe: StalkerBootstrapRecipe? = null,
    val endpointPreference: StalkerEndpointPreference = StalkerEndpointPreference.AUTO,
    val cookieMode: StalkerCookieMode = StalkerCookieMode.NONE,
    val playbackBackendHint: StalkerPlaybackBackendHint = StalkerPlaybackBackendHint.AUTO,
    val fallbackRecipeUsed: Boolean = false,
    val rediscoveryAttempted: Boolean = false
) : IOException(message, cause)
