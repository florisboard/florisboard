package dev.patrickgold.florisboard.ime.text.key

import android.annotation.SuppressLint
import com.squareup.moshi.FromJson

enum class KeyVariation {
    ALL,
    EMAIL_ADDRESS,
    NORMAL,
    PASSWORD,
    URI;

    companion object {
        @SuppressLint("DefaultLocale")
        fun fromString(string: String): KeyVariation {
            return valueOf(string.toUpperCase())
        }
    }
}

class KeyVariationAdapter {
    @FromJson
    fun fromJson(raw: String): KeyVariation {
        return KeyVariation.fromString(raw)
    }
}
