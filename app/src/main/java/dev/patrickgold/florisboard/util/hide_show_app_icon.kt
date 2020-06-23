package dev.patrickgold.florisboard.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

fun hideAppIcon(context: Context) {
    val pkg: PackageManager = context.packageManager
    pkg.setComponentEnabledSetting(
        ComponentName(context, "dev.patrickgold.florisboard.FlorisBoardImeLauncherAlias"),
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP
    )
}

fun showAppIcon(context: Context) {
    val pkg: PackageManager = context.packageManager
    pkg.setComponentEnabledSetting(
        ComponentName(context, "dev.patrickgold.florisboard.FlorisBoardImeLauncherAlias"),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP
    )
}
