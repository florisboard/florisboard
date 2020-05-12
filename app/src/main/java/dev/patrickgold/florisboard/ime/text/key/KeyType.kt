package dev.patrickgold.florisboard.ime.text.key

import android.annotation.SuppressLint
import com.squareup.moshi.FromJson

/**
 * Enum for declaring the type of the key.
 * List of possible key types:
 *  [Wikipedia](https://en.wikipedia.org/wiki/Keyboard_layout#Key_types)
 */
enum class KeyType {
    CHARACTER,
    MODIFIER,
    ENTER_EDITING,
    SYSTEM_GUI,
    NAVIGATION,
    FUNCTION,
    NUMERIC,
    LOCK;

    companion object {
        @SuppressLint("DefaultLocale")
        fun fromString(string: String): KeyType {
            return valueOf(string.toUpperCase())
        }
    }
}

class KeyTypeAdapter {
    @FromJson
    fun fromJson(raw: String): KeyType {
        return KeyType.fromString(raw)
    }
}
