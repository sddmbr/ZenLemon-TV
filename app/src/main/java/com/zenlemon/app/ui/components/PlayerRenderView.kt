package com.zenlemon.app.ui.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.zenlemon.player.PlayerEngine
import com.zenlemon.player.PlayerRenderSurfaceType
import com.zenlemon.player.PlayerSurfaceResizeMode

@Composable
fun PlayerRenderView(
    playerEngine: PlayerEngine,
    resizeMode: PlayerSurfaceResizeMode,
    modifier: Modifier = Modifier,
    surfaceType: PlayerRenderSurfaceType = PlayerRenderSurfaceType.AUTO,
    configureView: (View.() -> Unit)? = null
) {
    key(playerEngine, surfaceType) {
        AndroidView(
            factory = { context ->
                playerEngine.createRenderView(context, resizeMode, surfaceType).apply {
                    configureView?.invoke(this)
                }
            },
            update = { renderView ->
                playerEngine.bindRenderView(renderView, resizeMode)
            },
            onRelease = { renderView ->
                playerEngine.releaseRenderView(renderView)
            },
            modifier = modifier
        )
    }
}