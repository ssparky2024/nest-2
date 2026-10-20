package com.nest.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AppIcon(app: AppInfo, size: Int, showLabel: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(size.dp))
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                app.label,
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = (size + 20).dp),
            )
        }
    }
}

@Composable
fun ClockBlock(s: NestState, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var now by remember { mutableStateOf(Date()) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                now = Date()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(ctx, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        now = Date()
        onDispose { ctx.unregisterReceiver(receiver) }
    }

    val locale = Locale.getDefault()
    val timeText = SimpleDateFormat(if (s.clock24h) "HH:mm" else "h:mm", locale).format(now)
    val dateText = if (s.dateFormat == "none") "" else
        runCatching { SimpleDateFormat(s.dateFormat, locale).format(now) }.getOrDefault("")

    val hAlign = when (s.clockAlign) {
        "left" -> Alignment.Start
        "right" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val tAlign = when (s.clockAlign) {
        "left" -> TextAlign.Start
        "right" -> TextAlign.End
        else -> TextAlign.Center
    }
    val family = when (s.clockFont) {
        "sans" -> FontFamily.SansSerif
        "mono" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.Serif
    }
    val weight = when (s.clockWeight) {
        "light" -> FontWeight.Light
        "bold" -> FontWeight.Bold
        else -> FontWeight.Normal
    }

    Column(
        modifier.fillMaxWidth().padding(horizontal = 48.dp),
        horizontalAlignment = hAlign,
    ) {
        Text(
            timeText,
            color = Color(s.clockColor),
            fontSize = s.clockSize.sp,
            fontFamily = family,
            fontWeight = weight,
            letterSpacing = s.letterSpacing.sp,
            textAlign = tAlign,
            maxLines = 1,
        )
        if (dateText.isNotEmpty()) {
            Spacer(Modifier.height(s.dateSpacing.dp))
            Text(
                dateText,
                color = Color(s.dateColor),
                fontSize = s.dateSize.sp,
                fontFamily = family,
                textAlign = tAlign,
                maxLines = 1,
            )
        }
    }
}

/** Clock, free-positioned shortcuts, dock, and the background gesture surface. */
@Composable
fun HomeLayer(
    s: NestState,
    apps: Map<String, AppInfo>,
    drag: DragController,
    rootSize: IntSize,
    onAction: (String) -> Unit,
) {
    val ctx = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        // Gesture surface: swipes, double tap, long press on empty space.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(s.doubleTap, s.longPress) {
                    detectTapGestures(
                        onDoubleTap = { onAction(s.doubleTap) },
                        onLongPress = { onAction(s.longPress) },
                    )
                }
                .pointerInput(s.swipeDown, s.swipeUp) {
                    val threshold = 48.dp.toPx()
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            if (total > threshold) {
                                onAction(s.swipeDown)
                            } else if (total < -threshold) {
                                onAction(s.swipeUp)
                            }
                            total = 0f
                        },
                        onDragCancel = { total = 0f },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            total += dy
                        },
                    )
                },
        )

        ClockBlock(
            s,
            Modifier.offset { IntOffset(0, (s.clockY * rootSize.height).roundToInt()) },
        )

        // Free-positioned shortcuts
        s.home.forEach { item ->
            val app = apps[item.pkg] ?: return@forEach
            key(item.pkg) {
                DragItem(
                    pkg = item.pkg,
                    source = Source.HOME,
                    drag = drag,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (item.x * rootSize.width).roundToInt(),
                                (item.y * rootSize.height).roundToInt(),
                            )
                        }
                        .width((s.iconSize + 24).dp),
                    onTap = { launchApp(ctx, app) },
                ) {
                    AppIcon(app, s.iconSize, s.showLabels, Modifier.fillMaxWidth())
                }
            }
        }

        // Dock
        if (s.dockVisible && (s.dock.isNotEmpty() || drag.active)) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = if (drag.active) 0.18f else 0.08f))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    s.dock.forEach { pkg ->
                        val app = apps[pkg] ?: return@forEach
                        key(pkg) {
                            DragItem(
                                pkg = pkg,
                                source = Source.DOCK,
                                drag = drag,
                                onTap = { launchApp(ctx, app) },
                            ) {
                                AppIcon(app, s.dockIconSize, s.dockLabels)
                            }
                        }
                    }
                    if (s.dock.isEmpty()) {
                        Text(
                            "Drop apps here",
                            color = Color(0x99FFFFFF),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
