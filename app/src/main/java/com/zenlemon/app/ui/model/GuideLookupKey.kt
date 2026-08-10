package com.zenlemon.app.ui.model

import com.zenlemon.domain.model.Channel

fun Channel.guideLookupKey(): String? {
    return epgChannelId?.trim()?.takeIf { it.isNotEmpty() }
        ?: streamId.takeIf { it > 0L }?.toString()
}
