package dev.patrickgold.florisboard.ime.text.composing

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

interface Composer {
    val name: String
    val label: String
    val toRead: Int

    fun getActions(s: String, c: Char): Pair<Int, String>
}

@Serializable
@SerialName("appender")
class Appender : Composer {
    companion object {
        const val name = "appender"
    }
    override val name: String = Appender.name
    override val label: String = "Appender"
    override val toRead: Int = 0

    override fun getActions(s: String, c: Char): Pair<Int, String> {
        return Pair(0, "$c")
    }
}

@Serializable
@SerialName("with-rules")
class WithRules(
    override val name: String,
    override val label: String,
    val rules: JsonObject
) : Composer {
    override val toRead: Int = rules.keys.toList().sortedBy { it.length }.reversed()[0].length - 1

    @Transient val ruleOrder: List<String> = rules.keys.toList().sortedBy { it.length }.reversed()
    @Transient val ruleMap: Map<String, String> = rules.entries.map { Pair(it.key, (it.value as JsonPrimitive).content) }.toMap()

    override fun getActions(s: String, c: Char): Pair<Int, String> {
        val str = "${s}$c"
        for (key in ruleOrder) {
            if (str.endsWith(key)) {
                return Pair(key.length-1, ruleMap.getValue(key))
            }
        }
        return Pair(0, "$c")
    }
}
