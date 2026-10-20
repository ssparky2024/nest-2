package com.nest.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val ZONE_HEIGHT = 84.dp
private const val MAX_DOCK = 7

@Composable
fun NestApp(store: NestStore, homeTick: Int) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val s by store.state.collectAsState(initial = NestState())
    var apps by remember { mutableStateOf(emptyList<AppInfo>()) }
    var refresh by remember { mutableIntStateOf(0) }
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    val drag = remember { DragController() }
    val navBottom = WindowInsets.navigationBars.getBottom(density)

    // Reload apps when packages are installed / removed / changed
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                refresh++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(ctx, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose { ctx.unregisterReceiver(receiver) }
    }
    LaunchedEffect(refresh) {
        apps = withContext(Dispatchers.Default) { loadApps(ctx) }
    }

    // Pressing Home while Nest is open closes drawer / settings
    LaunchedEffect(homeTick) {
        if (homeTick > 0) {
            drawerOpen = false
            settingsOpen = false
        }
    }

    // Back never leaves the launcher; it only closes panels
    BackHandler {
        when {
            settingsOpen -> settingsOpen = false
            drawerOpen -> drawerOpen = false
        }
    }

    val appMap = remember(apps) { apps.associateBy { it.pkg } }

    // ----- Drop handling -----
    val zoneH = with(density) { ZONE_HEIGHT.toPx() }
    val itemW = with(density) { (s.iconSize + 24).dp.toPx() }
    val iconPx = with(density) { s.iconSize.dp.toPx() }
    val cellH = with(density) { (s.iconSize + 28).dp.toPx() }
    val dockZone = with(density) { (s.dockIconSize + 70).dp.toPx() } + navBottom
    val dockUnit = with(density) { (s.dockIconSize + 20).dp.toPx() }

    SideEffect {
        drag.onDrop = { pkg, source, at ->
            val w = rootSize.width.toFloat()
            val h = rootSize.height.toFloat()
            val zone = if (at.y < zoneH && w > 0f) ((at.x / w) * 3).toInt().coerceIn(0, 2) else -1
            val inDock = s.dockVisible && at.y > h - dockZone

            when {
                zone == 0 -> {
                    if (source != Source.DRAWER) {
                        scope.launch {
                            store.editLayout { d, hm -> (d - pkg) to hm.filter { it.pkg != pkg } }
                        }
                    }
                }
                zone == 1 -> openAppInfo(ctx, pkg)
                zone == 2 -> uninstallApp(ctx, pkg)
                inDock -> scope.launch {
                    store.editLayout { d, hm ->
                        val rest = d - pkg
                        val left = (w - rest.size * dockUnit) / 2f
                        val idx = ((at.x - left) / dockUnit).roundToInt().coerceIn(0, rest.size)
                        val nd = rest.toMutableList().apply { add(idx, pkg) }.take(MAX_DOCK)
                        nd to hm.filter { it.pkg != pkg }
                    }
                }
                w > 0f && h > 0f -> {
                    var x = at.x - itemW / 2f
                    var y = at.y - iconPx / 2f
                    if (s.snapToGrid) {
                        x = (x / itemW).roundToInt() * itemW
                        y = (y / cellH).roundToInt() * cellH
                    }
                    x = x.coerceIn(0f, (w - itemW).coerceAtLeast(0f))
                    y = y.coerceIn(0f, (h - cellH).coerceAtLeast(0f))
                    val fx = x / w
                    val fy = y / h
                    scope.launch {
                        store.editLayout { d, hm ->
                            (d - pkg) to (hm.filter { it.pkg != pkg } + HomeItem(pkg, fx, fy))
                        }
                    }
                }
            }
            if (source == Source.DRAWER && zone != 0) drawerOpen = false
        }
    }

    val onAction: (String) -> Unit = { action ->
        when (action) {
            "drawer" -> drawerOpen = true
            "settings" -> settingsOpen = true
        }
    }

    val background: Brush = when (s.bgMode) {
        "gradient" -> Brush.verticalGradient(listOf(Color(s.bgColor), Color(s.bgColor2)))
        "wallpaper" -> SolidColor(Color.Transparent)
        else -> SolidColor(Color(s.bgColor))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .onSizeChanged { rootSize = it },
    ) {
        HomeLayer(s, appMap, drag, rootSize, onAction)

        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn() + slideInVertically { -it / 8 },
            exit = fadeOut() + slideOutVertically { -it / 8 },
        ) {
            // Stay composed (but invisible) while dragging so the gesture isn't cancelled
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (drag.active) 0f else 1f }) {
                AppDrawer(
                    s = s,
                    apps = apps,
                    drag = drag,
                    onLaunch = { app ->
                        launchApp(ctx, app)
                        drawerOpen = false
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = settingsOpen,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
        ) {
            SettingsPanel(
                s = s,
                onSet = { k, v -> scope.launch { store.set(k, v) } },
                onClear = {
                    scope.launch {
                        store.editLayout { _, _ -> emptyList<String>() to emptyList<HomeItem>() }
                    }
                },
                onClose = { settingsOpen = false },
            )
        }

        if (drag.active) DropZones(drag, rootSize.width)

        // Drag ghost
        drag.pkg?.let { pkg ->
            appMap[pkg]?.let { app ->
                val half = with(density) { (s.iconSize.dp / 2).toPx() }
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (drag.pos.x - half).roundToInt(),
                                (drag.pos.y - half).roundToInt(),
                            )
                        }
                        .size(s.iconSize.dp)
                        .graphicsLayer {
                            scaleX = 1.2f
                            scaleY = 1.2f
                        },
                )
            }
        }
    }
}

@Composable
private fun DropZones(drag: DragController, width: Int) {
    val zoneHPx = with(LocalDensity.current) { ZONE_HEIGHT.toPx() }
    val hovered =
        if (drag.pos.y < zoneHPx && width > 0) ((drag.pos.x / width) * 3).toInt().coerceIn(0, 2) else -1
    val labels = listOf("Remove", "App info", "Uninstall")

    Row(Modifier.fillMaxWidth().height(ZONE_HEIGHT).background(Color(0xDD111111))) {
        labels.forEachIndexed { i, label ->
            val enabled = i != 0 || drag.source != Source.DRAWER
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (hovered == i && enabled) Color(0x44FFFFFF) else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (enabled) Color.White else Color(0x55FFFFFF),
                    fontSize = 16.sp,
                )
            }
        }
    }
}
