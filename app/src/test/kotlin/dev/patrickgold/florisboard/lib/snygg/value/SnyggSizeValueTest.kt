package dev.patrickgold.florisboard.lib.snygg.value

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.result.shouldBeFailureOfType
import io.kotest.matchers.result.shouldBeSuccess

class SnyggSizeValueTest : FreeSpec({
    val validPairs = listOf(
        "4.0dp"     to SnyggDpSizeValue(4.dp),
        "12.0dp"    to SnyggDpSizeValue(12.dp),
        "12.5dp"    to SnyggDpSizeValue(12.5.dp),
        "4.0sp"     to SnyggSpSizeValue(4.sp),
        "12.0sp"    to SnyggSpSizeValue(12.sp),
        "12.5sp"    to SnyggSpSizeValue(12.5.sp),
        "4.0%"      to SnyggPercentageSizeValue(0.04f),
        "12.0%"     to SnyggPercentageSizeValue(0.12f),
        "12.5%"     to SnyggPercentageSizeValue(0.125f),
        "100.0%"    to SnyggPercentageSizeValue(1.0f),
    )

    val invalidPairs = listOf(
        "-4.0dp"    to SnyggDpSizeValue((-4).dp),
        "-12.0dp"   to SnyggDpSizeValue((-12).dp),
        "-12.5dp"   to SnyggDpSizeValue((-12.5).dp),
        "-4.0sp"    to SnyggSpSizeValue((-4).sp),
        "-12.0sp"   to SnyggSpSizeValue((-12).sp),
        "-12.5sp"   to SnyggSpSizeValue((-12.5).sp),
        "-4.0%"     to SnyggPercentageSizeValue(-4.0f),
        "-12.0%"    to SnyggPercentageSizeValue(-12.0f),
        "-12.5%"    to SnyggPercentageSizeValue(-12.5f),
        "-100.0%"   to SnyggPercentageSizeValue(-100.0f),
        "101.0%"    to SnyggPercentageSizeValue(1.01f),
    )

    val validIllFormattedInput = listOf(
        SnyggDpSizeValue(5.dp) to listOf(" 5dp", " 5dp ", " 5 dp", "5.dp", "5. dp"),
        SnyggDpSizeValue(0.5.dp) to listOf(".5dp", " .5dp"),
    )

    val invalidInput = listOf(
        SnyggDpSizeValue to listOf(
            "", " ", "dp", "-dp", "5,0dp", "5Dp", "5DP", "5dP", "5d p", "5sp", "5px", "5%", "5",
        ),
        SnyggSpSizeValue to listOf(
            "", " ", "sp", "-sp", "5,0sp", "5Sp", "5SP", "5sP", "5s p", "5dp", "5px", "5%", "5",
        ),
    )

    "Test .deserialize()/.serialize() with valid input" - {
        validPairs.forEach { (raw, obj) ->
            "`$raw` should be success" {
                obj.encoder().deserialize(raw) shouldBeSuccess obj
                obj.encoder().serialize(obj) shouldBeSuccess raw
            }
        }
    }

    "Test .deserialize()/.serialize() with invalid input" - {
        invalidPairs.forEach { (raw, obj) ->
            "`$raw` should be failure" {
                obj.encoder().deserialize(raw).shouldBeFailureOfType<Exception>()
                obj.encoder().serialize(obj).shouldBeFailureOfType<Exception>()
            }
        }
    }

    "Test .deserialize() with valid, ill-formatted input" - {
        validIllFormattedInput.forEach { (obj, list) ->
            "${obj::class.qualifiedName}" - {
                list.forEach { raw ->
                    "`$raw` should be success" {
                        obj.encoder().deserialize(raw) shouldBeSuccess obj
                    }
                }
            }
        }
    }

    "Test .deserialize() with invalid input" - {
        invalidInput.forEach { (encoder, list) ->
            "${encoder::class.qualifiedName}" - {
                list.forEach { raw ->
                    "`$raw` should be failure" {
                        encoder.deserialize(raw).shouldBeFailureOfType<Exception>()
                    }
                }
            }
        }
    }
})
