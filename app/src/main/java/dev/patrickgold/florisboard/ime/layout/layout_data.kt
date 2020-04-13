package dev.patrickgold.florisboard.ime.layout

data class LayoutKeyPrimaryData(
    val code: Int,
    val label: String,
    val popup: List<LayoutKeySecondaryData>
)

data class LayoutKeySecondaryData(
    val code: Int,
    val label: String
)

data class LayoutData(
    val name: String,
    val direction: String,
    val arrangement: List<List<LayoutKeyPrimaryData>>
)
