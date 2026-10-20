package com.nest.launcher

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

enum class Source { DRAWER, HOME, DOCK }

/** Shared state for "long-press, then drag" of an app icon from anywhere to anywhere. */
class DragController {
    var pkg by mutableStateOf<String?>(null)
        private set
    var source by mutableStateOf(Source.DRAWER)
        private set
    var pos by mutableStateOf(Offset.Zero)
        private set

    /** Taps are ignored until this uptime, so releasing a drag never launches the app. */
    var suppressTapUntil = 0L
    var onDrop: (String, Source, Offset) -> Unit = { _, _, _ -> }

    val active: Boolean get() = pkg != null

    fun start(p: String, s: Source, at: Offset) {
        pkg = p; source = s; pos = at
        suppressTapUntil = Long.MAX_VALUE
    }

    fun move(delta: Offset) {
        pos += delta
    }

    fun end() {
        val p = pkg ?: return
        val s = source
        val at = pos
        pkg = null
        suppressTapUntil = SystemClock.uptimeMillis() + 400
        onDrop(p, s, at)
    }

    fun cancel() {
        pkg = null
        suppressTapUntil = SystemClock.uptimeMillis() + 400
    }
}

private class CoordHolder { var c: LayoutCoordinates? = null }

@Composable
fun DragItem(
    pkg: String,
    source: Source,
    drag: DragController,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    content: @Composable () -> Unit,
) {
    val holder = remember { CoordHolder() }
    val hidden = drag.pkg == pkg && drag.source == source
    Box(
        modifier
            .graphicsLayer { alpha = if (hidden) 0f else 1f }
            .onGloballyPositioned { holder.c = it }
            .pointerInput(pkg, source) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { off ->
                        drag.start(pkg, source, holder.c?.localToRoot(off) ?: off)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        drag.move(amount)
                    },
                    onDragEnd = { drag.end() },
                    onDragCancel = { drag.cancel() },
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (SystemClock.uptimeMillis() >= drag.suppressTapUntil) onTap()
            },
    ) { content() }
}
