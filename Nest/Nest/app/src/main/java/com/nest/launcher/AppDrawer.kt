package com.nest.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawer(
    s: NestState,
    apps: List<AppInfo>,
    drag: DragController,
    onLaunch: (AppInfo) -> Unit,
) {
    var query by remember { mutableStateOf("") }

    val sorted = remember(apps, s.drawerSort) {
        when (s.drawerSort) {
            "za" -> apps.sortedByDescending { it.label.lowercase() }
            "newest" -> apps.sortedByDescending { it.installed }
            else -> apps.sortedBy { it.label.lowercase() }
        }
    }
    val shown = remember(sorted, query) {
        if (query.isBlank()) sorted else sorted.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(s.bgColor).copy(alpha = s.drawerAlpha))
            .pointerInput(Unit) { detectTapGestures { } } // keep touches off the home screen below
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        if (s.drawerShowSearch) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search apps") },
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { shown.firstOrNull()?.let(onLaunch) }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(s.drawerColumns.coerceAtLeast(1)),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(s.drawerSpacing.dp),
            horizontalArrangement = Arrangement.spacedBy(s.drawerSpacing.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(shown, key = { it.pkg }) { app ->
                DragItem(
                    pkg = app.pkg,
                    source = Source.DRAWER,
                    drag = drag,
                    modifier = Modifier.fillMaxWidth(),
                    onTap = { onLaunch(app) },
                ) {
                    AppIcon(app, s.drawerIconSize, s.showLabels, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
