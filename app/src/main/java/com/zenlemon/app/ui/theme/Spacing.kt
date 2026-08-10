package com.zenlemon.app.ui.theme

import com.zenlemon.app.ui.design.AppSpacing
import com.zenlemon.app.ui.design.LocalAppSpacing

typealias Spacing = AppSpacing

val LocalSpacing = LocalAppSpacing

fun defaultSpacing(): Spacing = AppSpacing()
