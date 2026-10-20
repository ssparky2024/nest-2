package com.nest.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val FONTS = listOf("serif" to "Serif", "sans" to "Sans", "mono" to "Mono", "cursive" to "Cursive")
private val WEIGHTS = listOf("light" to "Light", "normal" to "Regular", "bold" to "Bold")
private val ALIGNS = listOf("left" to "Left", "center" to "Center", "right" to "Right")
private val DATE_FORMATS = listOf(
    "EEEE, MMMM d" to "Monday, May 4",
    "EEE, MMM d" to "Mon, May 4",
    "MMMM d, yyyy" to "May 4, 2026",
    "d MMMM" to "4 May",
    "MM/dd/yyyy" to "05/04/2026",
    "yyyy-MM-dd" to "2026-05-04",
    "none" to "Hidden",
)
private val BG_MODES = listOf("color" to "Solid", "gradient" to "Gradient", "wallpaper" to "Wallpaper")
private val SORTS = listOf("az" to "A-Z", "za" to "Z-A", "newest" to "Newest")
private val ACTIONS = listOf("drawer" to "App drawer", "settings" to "Settings", "none" to "Nothing")

private val PALETTE = listOf(
    0xFFFFFFFF, 0xFFF5F0E6, 0xFFFFD60A, 0xFFFF9F0A, 0xFFFF453A, 0xFFFF375F, 0xFFBF5AF2,
    0xFF5E5CE6, 0xFF0A84FF, 0xFF64D2FF, 0xFF30D158, 0xFF8E8E93, 0xFF1B1B2F, 0xFF000000,
).map { it.toInt() }

@Composable
fun SettingsPanel(
    s: NestState,
    onSet: (String, Any) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        // Tap outside the panel to close
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClose() },
        )
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures { } },
            color = Color(0xF2121212),
        ) {
            Column(
                Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Nest settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onClose) { Text("Done") }
                }

                Section("Clock")
                ChoiceRow("Font", FONTS, s.clockFont) { onSet("clockFont", it) }
                ChoiceRow("Weight", WEIGHTS, s.clockWeight) { onSet("clockWeight", it) }
                IntSlider("Size", s.clockSize, 40, 240, " sp") { onSet("clockSize", it) }
                IntSlider("Letter spacing", s.letterSpacing, -4, 24, "") { onSet("letterSpacing", it) }
                FloatSlider("Vertical position", s.clockY, 0f, 0.85f, { "${(it * 100).roundToInt()}%" }) { onSet("clockY", it) }
                ChoiceRow("Alignment", ALIGNS, s.clockAlign) { onSet("clockAlign", it) }
                SwitchRow("24-hour time", s.clock24h) { onSet("clock24h", it) }
                ColorRow("Color", s.clockColor) { onSet("clockColor", it) }

                Section("Date")
                ChoiceRow("Format", DATE_FORMATS, s.dateFormat) { onSet("dateFormat", it) }
                IntSlider("Size", s.dateSize, 10, 60, " sp") { onSet("dateSize", it) }
                IntSlider("Gap below clock", s.dateSpacing, 0, 60, " dp") { onSet("dateSpacing", it) }
                ColorRow("Color", s.dateColor) { onSet("dateColor", it) }

                Section("Background")
                ChoiceRow("Style", BG_MODES, s.bgMode) { onSet("bgMode", it) }
                if (s.bgMode != "wallpaper") ColorRow("Color", s.bgColor) { onSet("bgColor", it) }
                if (s.bgMode == "gradient") ColorRow("Gradient end", s.bgColor2) { onSet("bgColor2", it) }

                Section("Home icons")
                IntSlider("Icon size", s.iconSize, 32, 104, " dp") { onSet("iconSize", it) }
                SwitchRow("Show labels", s.showLabels) { onSet("showLabels", it) }
                SwitchRow("Snap to grid", s.snapToGrid) { onSet("snapToGrid", it) }

                Section("Dock")
                SwitchRow("Show dock", s.dockVisible) { onSet("dockVisible", it) }
                IntSlider("Icon size", s.dockIconSize, 32, 90, " dp") { onSet("dockIconSize", it) }
                SwitchRow("Show labels", s.dockLabels) { onSet("dockLabels", it) }

                Section("App drawer")
                IntSlider("Columns", s.drawerColumns, 3, 12, "") { onSet("drawerColumns", it) }
                IntSlider("Icon size", s.drawerIconSize, 32, 104, " dp") { onSet("drawerIconSize", it) }
                IntSlider("Spacing", s.drawerSpacing, 0, 40, " dp") { onSet("drawerSpacing", it) }
                FloatSlider("Background opacity", s.drawerAlpha, 0.4f, 1f, { "${(it * 100).roundToInt()}%" }) { onSet("drawerAlpha", it) }
                ChoiceRow("Sort", SORTS, s.drawerSort) { onSet("drawerSort", it) }
                SwitchRow("Search bar", s.drawerShowSearch) { onSet("drawerShowSearch", it) }

                Section("Gestures")
                ChoiceRow("Swipe down", ACTIONS, s.swipeDown) { onSet("swipeDown", it) }
                ChoiceRow("Swipe up", ACTIONS, s.swipeUp) { onSet("swipeUp", it) }
                ChoiceRow("Double tap", ACTIONS, s.doubleTap) { onSet("doubleTap", it) }
                ChoiceRow("Long press", ACTIONS, s.longPress) { onSet("longPress", it) }

                Section("Reset")
                TextButton(onClick = onClear) {
                    Text("Clear home screen and dock", color = Color(0xFFFF453A))
                }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(),
        color = Color(0xFF8E8E93),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 24.dp, bottom = 6.dp),
    )
}

@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color(0xFFE8E8E8) else Color(0xFF2A2A2A))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, color = if (selected) Color.Black else Color.White, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, color = Color.White, fontSize = 15.sp)
        FlowRow(
            Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (value, name) -> Pill(name, value == selected) { onSelect(value) } }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun FloatSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    var v by remember { mutableFloatStateOf(value) }
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 15.sp)
            Text(format(v), color = Color(0xFF8E8E93), fontSize = 14.sp)
        }
        Slider(
            value = v.coerceIn(min, max),
            onValueChange = { v = it; onChange(it) },
            valueRange = min..max,
        )
    }
}

@Composable
private fun IntSlider(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    suffix: String,
    onChange: (Int) -> Unit,
) {
    FloatSlider(
        label = label,
        value = value.toFloat(),
        min = min.toFloat(),
        max = max.toFloat(),
        format = { "${it.roundToInt()}$suffix" },
    ) { onChange(it.roundToInt()) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorRow(label: String, color: Int, onChange: (Int) -> Unit) {
    var hex by remember(color) { mutableStateOf("#%06X".format(color and 0xFFFFFF)) }
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, color = Color.White, fontSize = 15.sp)
        FlowRow(
            Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PALETTE.forEach { c ->
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            if (c == color) 2.dp else 1.dp,
                            if (c == color) Color.White else Color(0x55FFFFFF),
                            CircleShape,
                        )
                        .clickable { onChange(c) },
                )
            }
        }
        OutlinedTextField(
            value = hex,
            onValueChange = { t ->
                hex = t
                runCatching { android.graphics.Color.parseColor(t) }.onSuccess(onChange)
            },
            singleLine = true,
            label = { Text("Hex") },
            modifier = Modifier.width(170.dp).padding(top = 8.dp),
        )
    }
}
