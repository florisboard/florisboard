package dev.patrickgold.florisboard.app.layoutbuilder

object LayoutValidation {
    private val allowedSpecialCodes = setOf(
        "SHIFT_TOGGLE",
        "MODE_SYMBOLS",
        "CTRL_MOD",
        "MENU_TOGGLE",
    )

    fun validatePack(pack: LayoutPack): List<String> {
        val errors = mutableListOf<String>()
        for ((index, row) in pack.rows.withIndex()) {
            val rowErrors = validateRow(row, pack.units)
            if (rowErrors.isNotEmpty()) {
                errors += rowErrors.map { error -> "Row ${row.id.ifEmpty { index.toString() }}: $error" }
            }
        }
        return errors
    }

    fun validateRow(row: LayoutRow, expectedUnits: Int = row.units): List<String> {
        val errors = mutableListOf<String>()
        val sumUnits = row.keys.sumOf { it.units }
        if (sumUnits != expectedUnits) {
            errors += "Σu $sumUnits/$expectedUnits"
        }
        for (key in row.keys) {
            if (!isValidCode(key.code)) {
                errors += "Invalid code '${key.code}'"
            }
        }
        return errors
    }

    private fun isValidCode(code: String): Boolean {
        if (code.isBlank()) return false
        val trimmed = code.trim()
        val codePointCount = trimmed.codePointCount(0, trimmed.length)
        if (codePointCount == 1 && !trimmed.startsWith("KEYCODE_")) {
            return true
        }
        if (trimmed.startsWith("KEYCODE_")) {
            return trimmed.length > "KEYCODE_".length
        }
        if (allowedSpecialCodes.contains(trimmed)) {
            return true
        }
        return false
    }
}
