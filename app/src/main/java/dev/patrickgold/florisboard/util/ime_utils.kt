package dev.patrickgold.florisboard.util

import android.content.Context
import android.provider.Settings
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.debug.flogInfo

private const val IME_ID: String = "dev.patrickgold.florisboard/.FlorisImeService"
private const val IME_ID_BETA: String =
    "dev.patrickgold.florisboard.beta/dev.patrickgold.florisboard.FlorisImeService"
private const val IME_ID_DEBUG: String =
    "dev.patrickgold.florisboard.debug/dev.patrickgold.florisboard.FlorisImeService"

fun checkIfImeIsEnabled(context: Context): Boolean {
    val activeImeIds = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_INPUT_METHODS
    ) ?: "(none)"
    flogInfo { "List of active IMEs: $activeImeIds" }
    return when {
        BuildConfig.DEBUG -> {
            activeImeIds.split(":").contains(IME_ID_DEBUG)
        }
        context.packageName.endsWith(".beta") -> {
            activeImeIds.split(":").contains(IME_ID_BETA)
        }
        else -> {
            activeImeIds.split(":").contains(IME_ID)
        }
    }
}

fun checkIfImeIsSelected(context: Context): Boolean {
    val selectedImeId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    ) ?: "(none)"
    flogInfo { "Selected IME: $selectedImeId" }
    return when {
        BuildConfig.DEBUG -> {
            selectedImeId == IME_ID_DEBUG
        }
        context.packageName.endsWith(".beta") -> {
            selectedImeId.split(":").contains(IME_ID_BETA)
        }
        else -> {
            selectedImeId == IME_ID
        }
    }
}
