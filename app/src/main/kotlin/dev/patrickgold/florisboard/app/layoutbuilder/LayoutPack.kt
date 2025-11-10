package dev.patrickgold.florisboard.app.layoutbuilder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LayoutPack(
    val id: String,
    val label: String,
    val units: Int = DefaultUnits,
    val rows: List<LayoutRow> = emptyList(),
) {
    companion object {
        const val DefaultUnits: Int = 12
    }
}

@Serializable
data class LayoutRow(
    val id: String,
    val units: Int = LayoutPack.DefaultUnits,
    val showIfSetting: String? = null,
    val enabled: Boolean = true,
    val keys: List<LayoutKey> = emptyList(),
)

@Serializable
data class LayoutKey(
    val id: String? = null,
    val label: String = "",
    val code: String = "",
    val units: Int = 1,
    val style: LayoutKeyStyle = LayoutKeyStyle.DEFAULT,
    val spacer: Boolean = false,
)

@Serializable
enum class LayoutKeyStyle {
    @SerialName("default")
    DEFAULT,

    @SerialName("aux")
    AUX,

    @SerialName("special_left")
    SPECIAL_LEFT,

    @SerialName("special_right")
    SPECIAL_RIGHT,

    @SerialName("space")
    SPACE,
}
