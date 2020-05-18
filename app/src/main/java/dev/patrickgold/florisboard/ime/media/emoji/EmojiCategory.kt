package dev.patrickgold.florisboard.ime.media.emoji

import android.annotation.SuppressLint
import com.squareup.moshi.FromJson

/**
 * Enum for emoji category.
 * List taken from https://unicode.org/Public/emoji/13.0/emoji-test.txt
 */
enum class EmojiCategory {
    SMILEYS_EMOTION,
    PEOPLE_BODY,
    ANIMALS_NATURE,
    FOOD_DRINK,
    TRAVEL_PLACES,
    ACTIVITIES,
    OBJECTS,
    SYMBOLS,
    FLAGS;

    override fun toString(): String {
        return super.toString().replace("_", " & ")
    }

    companion object {
        @SuppressLint("DefaultLocale")
        fun fromString(string: String): EmojiCategory {
            return valueOf(string.replace(" & ", "_").toUpperCase())
        }
    }
}

class EmojiCategoryAdapter {
    @FromJson
    fun fromJson(raw: String): EmojiCategory {
        return EmojiCategory.fromString(raw)
    }
}
