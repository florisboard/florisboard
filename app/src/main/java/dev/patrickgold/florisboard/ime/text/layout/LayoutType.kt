package dev.patrickgold.florisboard.ime.text.layout

import com.squareup.moshi.FromJson
import java.util.*

/**
 * Defines the type of the layout.
 */
enum class LayoutType {
    CHARACTERS,
    CHARACTERS_MOD,
    EXTENSION,
    NUMERIC,
    NUMERIC_ADVANCED,
    PHONE,
    PHONE2,
    SYMBOLS,
    SYMBOLS_MOD,
    SYMBOLS2,
    SYMBOLS2_MOD;

    override fun toString(): String {
        return super.toString().replace("_", "/").toLowerCase(Locale.ENGLISH)
    }

    companion object {
        fun fromString(string: String): LayoutType {
            return valueOf(string.replace("/", "_").toUpperCase(Locale.ENGLISH))
        }
    }
}

class LayoutTypeAdapter {
    @FromJson
    fun fromJson(raw: String): LayoutType {
        return LayoutType.fromString(raw)
    }
}
