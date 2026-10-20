package com.nest.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(
    val pkg: String,
    val label: String,
    val component: ComponentName,
    val icon: ImageBitmap,
    val installed: Long,
)

fun loadApps(ctx: Context): List<AppInfo> {
    val pm = ctx.packageManager
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(query, 0)
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map { ri ->
            val pkg = ri.activityInfo.packageName
            AppInfo(
                pkg = pkg,
                label = ri.loadLabel(pm).toString(),
                component = ComponentName(pkg, ri.activityInfo.name),
                icon = ri.loadIcon(pm).toBitmap(144, 144).asImageBitmap(),
                installed = runCatching { pm.getPackageInfo(pkg, 0).firstInstallTime }.getOrDefault(0L),
            )
        }
        .distinctBy { it.pkg }
}

fun launchApp(ctx: Context, app: AppInfo) {
    val i = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(app.component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    runCatching { ctx.startActivity(i) }
}

fun openAppInfo(ctx: Context, pkg: String) {
    val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(i) }
}

fun uninstallApp(ctx: Context, pkg: String) {
    val i = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(i) }
}
