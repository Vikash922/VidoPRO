package com.example.core.media.blend

import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Xfermode
import com.example.core.model.BlendMode

/**
 * Universal mapping for [BlendMode] across Compose Preview, Android Canvas, and Media3 Export.
 */
object BlendModeHelper {

    fun toPorterDuffMode(blendMode: BlendMode): PorterDuff.Mode = when (blendMode) {
        BlendMode.NORMAL -> PorterDuff.Mode.SRC_OVER
        BlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
        BlendMode.SCREEN -> PorterDuff.Mode.SCREEN
        BlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
        BlendMode.DARKEN -> PorterDuff.Mode.DARKEN
        BlendMode.LIGHTEN -> PorterDuff.Mode.LIGHTEN
        BlendMode.ADD -> PorterDuff.Mode.ADD
    }

    fun toXfermode(blendMode: BlendMode): Xfermode =
        PorterDuffXfermode(toPorterDuffMode(blendMode))

    fun toComposeBlendMode(blendMode: BlendMode): androidx.compose.ui.graphics.BlendMode = when (blendMode) {
        BlendMode.NORMAL -> androidx.compose.ui.graphics.BlendMode.SrcOver
        BlendMode.MULTIPLY -> androidx.compose.ui.graphics.BlendMode.Multiply
        BlendMode.SCREEN -> androidx.compose.ui.graphics.BlendMode.Screen
        BlendMode.OVERLAY -> androidx.compose.ui.graphics.BlendMode.Overlay
        BlendMode.DARKEN -> androidx.compose.ui.graphics.BlendMode.Darken
        BlendMode.LIGHTEN -> androidx.compose.ui.graphics.BlendMode.Lighten
        BlendMode.ADD -> androidx.compose.ui.graphics.BlendMode.Plus
    }
}
