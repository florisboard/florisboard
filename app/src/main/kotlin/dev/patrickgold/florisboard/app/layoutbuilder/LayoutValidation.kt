package dev.patrickgold.florisboard.app.layoutbuilder

import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData

object LayoutValidation {
    private val validTextKeyDataLabels = TextKeyData.InternalKeys.map { it.label }.toSet()

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

        // 1. Check if it's a single character
        if (codePointCount == 1) {
            return true
        }

        // 2. Check if it's a predefined TextKeyData label
        if (validTextKeyDataLabels.contains(trimmed)) {
            return true
        }

        // 3. Check if it's a raw KeyCode integer
        val intCode = trimmed.toIntOrNull()
        if (intCode != null) {
            // For simplicity, we'll allow any integer for now.
            // A more robust check would involve a map of KeyCode integer values.
            return true
        }

        return false
    }
}
