package dev.patrickgold.florisboard.ime.text.composing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

interface Composer {
    val id: String
    val label: String
    val toRead: Int

    fun getActions(s: String, c: Char): Pair<Int, String>
}

@Serializable
@SerialName("appender")
class Appender : Composer {
    companion object {
        val DefaultInstance = Appender()
    }

    override val id: String = "appender"
    override val label: String = "Appender"
    override val toRead: Int = 0

    override fun getActions(s: String, c: Char): Pair<Int, String> {
        return 0 to c.toString()
    }
}

@Serializable
@SerialName("with-rules")
class WithRules(
    override val id: String,
    override val label: String,
    val rules: JsonObject,
) : Composer {
    override val toRead: Int = rules.keys.toList().sortedBy { it.length }.reversed()[0].length - 1

    @Transient val ruleOrder: List<String> = rules.keys.toList().sortedBy { it.length }.reversed()
    @Transient val ruleMap: Map<String, String> =
        rules.entries.associate { it.key to (it.value as JsonPrimitive).content }

    override fun getActions(s: String, c: Char): Pair<Int, String> {
        val str = "${s}$c"
        for (key in ruleOrder) {
            if (str.lowercase().endsWith(key)) {
                val value = ruleMap.getValue(key)
                val firstOfKey = str.takeLast(key.length).take(1)
                return (key.length-1) to (if (firstOfKey.uppercase().equals(firstOfKey, false)) value.uppercase() else value)
            }
        }
        return 0 to c.toString()
    }
}
