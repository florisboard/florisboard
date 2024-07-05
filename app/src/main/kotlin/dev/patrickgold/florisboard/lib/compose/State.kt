package dev.patrickgold.florisboard.lib.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.florisboard.lib.android.AndroidSettingsHelper
import org.florisboard.lib.android.SystemSettingsObserver

@Composable
fun AndroidSettingsHelper.observeAsState(
    key: String,
    foregroundOnly: Boolean = false,
) = observeAsState(
    key = key,
    foregroundOnly = foregroundOnly,
    transform = { it },
)

@Composable
fun <R> AndroidSettingsHelper.observeAsState(
    key: String,
    foregroundOnly: Boolean = false,
    transform: (String?) -> R,
): State<R> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current.applicationContext
    val state = remember(key) { mutableStateOf(transform(getString(context, key))) }
    DisposableEffect(lifecycleOwner.lifecycle) {
        val observer = SystemSettingsObserver(context) {
            state.value = transform(getString(context, key))
        }
        if (foregroundOnly) {
            val eventObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        observe(context, key, observer)
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        removeObserver(context, observer)
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(eventObserver)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(eventObserver)
                removeObserver(context, observer)
            }
        } else {
            observe(context, key, observer)
            onDispose {
                removeObserver(context, observer)
            }
        }
    }
    return state
}
