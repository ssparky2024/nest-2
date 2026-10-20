package com.nest.launcher

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nest_prefs")

private val DOCK_KEY = stringPreferencesKey("dock")
private val HOME_KEY = stringPreferencesKey("home")

/** A shortcut placed on the home screen. x/y are fractions (0..1) of the screen size. */
data class HomeItem(val pkg: String, val x: Float, val y: Float)

data class NestState(
    // Clock
    val clockFont: String = "serif",
    val clockWeight: String = "light",
    val clockSize: Int = 110,
    val clockColor: Int = 0xFFFFFFFF.toInt(),
    val clockAlign: String = "center",
    val clock24h: Boolean = false,
    val clockY: Float = 0.28f,
    val letterSpacing: Int = 0,
    // Date
    val dateFormat: String = "EEEE, MMMM d",
    val dateSize: Int = 22,
    val dateColor: Int = 0xCCFFFFFF.toInt(),
    val dateSpacing: Int = 4,
    // Background
    val bgMode: String = "color",
    val bgColor: Int = 0xFF000000.toInt(),
    val bgColor2: Int = 0xFF1B1B2F.toInt(),
    // Home icons
    val iconSize: Int = 56,
    val showLabels: Boolean = true,
    val snapToGrid: Boolean = false,
    // Dock
    val dockVisible: Boolean = true,
    val dockIconSize: Int = 52,
    val dockLabels: Boolean = false,
    // Drawer
    val drawerColumns: Int = 6,
    val drawerIconSize: Int = 60,
    val drawerSpacing: Int = 12,
    val drawerSort: String = "az",
    val drawerShowSearch: Boolean = true,
    val drawerAlpha: Float = 0.96f,
    // Gestures: "drawer" | "settings" | "none"
    val swipeDown: String = "drawer",
    val swipeUp: String = "none",
    val doubleTap: String = "none",
    val longPress: String = "settings",
    // Content
    val dock: List<String> = emptyList(),
    val home: List<HomeItem> = emptyList(),
) {
    companion object {
        fun from(p: Preferences): NestState {
            val d = NestState()
            return NestState(
                clockFont = p.s("clockFont", d.clockFont),
                clockWeight = p.s("clockWeight", d.clockWeight),
                clockSize = p.i("clockSize", d.clockSize),
                clockColor = p.i("clockColor", d.clockColor),
                clockAlign = p.s("clockAlign", d.clockAlign),
                clock24h = p.b("clock24h", d.clock24h),
                clockY = p.f("clockY", d.clockY),
                letterSpacing = p.i("letterSpacing", d.letterSpacing),
                dateFormat = p.s("dateFormat", d.dateFormat),
                dateSize = p.i("dateSize", d.dateSize),
                dateColor = p.i("dateColor", d.dateColor),
                dateSpacing = p.i("dateSpacing", d.dateSpacing),
                bgMode = p.s("bgMode", d.bgMode),
                bgColor = p.i("bgColor", d.bgColor),
                bgColor2 = p.i("bgColor2", d.bgColor2),
                iconSize = p.i("iconSize", d.iconSize),
                showLabels = p.b("showLabels", d.showLabels),
                snapToGrid = p.b("snapToGrid", d.snapToGrid),
                dockVisible = p.b("dockVisible", d.dockVisible),
                dockIconSize = p.i("dockIconSize", d.dockIconSize),
                dockLabels = p.b("dockLabels", d.dockLabels),
                drawerColumns = p.i("drawerColumns", d.drawerColumns),
                drawerIconSize = p.i("drawerIconSize", d.drawerIconSize),
                drawerSpacing = p.i("drawerSpacing", d.drawerSpacing),
                drawerSort = p.s("drawerSort", d.drawerSort),
                drawerShowSearch = p.b("drawerShowSearch", d.drawerShowSearch),
                drawerAlpha = p.f("drawerAlpha", d.drawerAlpha),
                swipeDown = p.s("swipeDown", d.swipeDown),
                swipeUp = p.s("swipeUp", d.swipeUp),
                doubleTap = p.s("doubleTap", d.doubleTap),
                longPress = p.s("longPress", d.longPress),
                dock = readDock(p),
                home = readHome(p),
            )
        }
    }
}

private fun Preferences.s(name: String, d: String): String = this[stringPreferencesKey(name)] ?: d
private fun Preferences.i(name: String, d: Int): Int = this[intPreferencesKey(name)] ?: d
private fun Preferences.f(name: String, d: Float): Float = this[floatPreferencesKey(name)] ?: d
private fun Preferences.b(name: String, d: Boolean): Boolean = this[booleanPreferencesKey(name)] ?: d

private fun readDock(p: Preferences): List<String> =
    p[DOCK_KEY].orEmpty().split(",").filter { it.isNotBlank() }

private fun readHome(p: Preferences): List<HomeItem> =
    p[HOME_KEY].orEmpty().split(";").mapNotNull { entry ->
        val t = entry.split("|")
        if (t.size != 3) return@mapNotNull null
        val x = t[1].toFloatOrNull() ?: return@mapNotNull null
        val y = t[2].toFloatOrNull() ?: return@mapNotNull null
        HomeItem(t[0], x, y)
    }

private fun MutablePreferences.putAny(name: String, v: Any) {
    when (v) {
        is String -> this[stringPreferencesKey(name)] = v
        is Int -> this[intPreferencesKey(name)] = v
        is Float -> this[floatPreferencesKey(name)] = v
        is Boolean -> this[booleanPreferencesKey(name)] = v
    }
}

class NestStore(private val ctx: Context) {
    val state: Flow<NestState> = ctx.dataStore.data.map { NestState.from(it) }

    suspend fun set(name: String, value: Any) {
        ctx.dataStore.edit { it.putAny(name, value) }
    }

    /** Atomically rewrite dock + home together. */
    suspend fun editLayout(
        f: (dock: List<String>, home: List<HomeItem>) -> Pair<List<String>, List<HomeItem>>,
    ) {
        ctx.dataStore.edit { p ->
            val (d, h) = f(readDock(p), readHome(p))
            p[DOCK_KEY] = d.joinToString(",")
            p[HOME_KEY] = h.joinToString(";") { "${it.pkg}|${it.x}|${it.y}" }
        }
    }
}
