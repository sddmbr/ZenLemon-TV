package com.zenlemon.app.ui.screens.settings

import com.zenlemon.domain.model.RemoteColorButton
import com.zenlemon.domain.model.RemoteShortcutProfile

internal data class RemoteShortcutDialogTarget(
    val profile: RemoteShortcutProfile,
    val button: RemoteColorButton
) {
    fun storageKey(): String = "${profile.storageValue}:${button.storageValue}"

    companion object {
        fun fromStorageKey(value: String?): RemoteShortcutDialogTarget? {
            val parts = value?.split(':') ?: return null
            if (parts.size != 2) return null
            val profile = RemoteShortcutProfile.fromStorage(parts[0]) ?: return null
            val button = RemoteColorButton.fromStorage(parts[1]) ?: return null
            return RemoteShortcutDialogTarget(profile, button)
        }
    }
}
