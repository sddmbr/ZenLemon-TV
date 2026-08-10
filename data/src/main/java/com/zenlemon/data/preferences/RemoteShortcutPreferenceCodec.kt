package com.zenlemon.data.preferences

import com.zenlemon.domain.model.RemoteColorButton
import com.zenlemon.domain.model.RemoteShortcutPreferences
import com.zenlemon.domain.model.RemoteShortcutProfile
import com.zenlemon.domain.model.RemoteShortcutSelection
import com.zenlemon.domain.model.RemoteShortcutAction

internal const val REMOTE_SHORTCUT_USE_GLOBAL_SENTINEL = "__global_default__"

internal fun decodeRemoteShortcutSelection(
    profile: RemoteShortcutProfile,
    rawValue: String?
): RemoteShortcutSelection = when {
    rawValue.isNullOrBlank() -> RemoteShortcutSelection.profileDefault()
    rawValue == REMOTE_SHORTCUT_USE_GLOBAL_SENTINEL && profile != RemoteShortcutProfile.GLOBAL ->
        RemoteShortcutSelection.globalDefault()
    else -> RemoteShortcutAction.fromStorage(rawValue)
        ?.let(RemoteShortcutSelection::explicit)
        ?: RemoteShortcutSelection.profileDefault()
}

internal fun encodeRemoteShortcutSelection(
    profile: RemoteShortcutProfile,
    selection: RemoteShortcutSelection
): String? {
    val normalized = selection.normalizedForProfile(profile)
    return when (normalized.mode) {
        com.zenlemon.domain.model.RemoteShortcutSelectionMode.PROFILE_DEFAULT -> null
        com.zenlemon.domain.model.RemoteShortcutSelectionMode.GLOBAL_DEFAULT -> REMOTE_SHORTCUT_USE_GLOBAL_SENTINEL
        com.zenlemon.domain.model.RemoteShortcutSelectionMode.ACTION -> normalized.action?.storageValue
    }
}

internal fun decodeRemoteShortcutPreferences(
    read: (RemoteShortcutProfile, RemoteColorButton) -> String?
): RemoteShortcutPreferences = RemoteShortcutPreferences(
    selections = RemoteShortcutProfile.entries.associateWith { profile ->
        RemoteColorButton.entries.associateWith { button ->
            decodeRemoteShortcutSelection(profile, read(profile, button))
        }
    }
)
