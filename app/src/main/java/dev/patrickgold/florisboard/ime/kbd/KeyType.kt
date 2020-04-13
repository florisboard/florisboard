package dev.patrickgold.florisboard.ime.kbd

import android.annotation.SuppressLint
import com.squareup.moshi.FromJson
import java.lang.Error

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

    @SuppressLint("DefaultLocale")
    @FromJson
    fun fromJson(string: String): KeyType {
        return when (string.toUpperCase()) {
            "CHARACTER"     -> CHARACTER
            "MODIFIER"      -> MODIFIER
            "ENTER_EDITING" -> ENTER_EDITING
            "SYSTEM_GUI"    -> SYSTEM_GUI
            "NAVIGATION"    -> NAVIGATION
            "FUNCTION"      -> FUNCTION
            "NUMERIC"       -> NUMERIC
            "LOCK"          -> LOCK
            else            -> throw Error("Specified key type '$string' not valid!")
        }
    }
}
