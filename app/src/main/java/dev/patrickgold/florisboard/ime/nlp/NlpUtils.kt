package dev.patrickgold.florisboard.ime.nlp

abstract class NlpUtils {
    companion object {
        fun editDistance(x: String, y: String): Int {
            val dp = Array(x.length + 1) { IntArray(y.length + 1) { 0 } }

            for (i in 0..x.length) {
                for (j in 0..y.length) {
                    when {
                        i == 0 -> {
                            dp[i][j] = j
                        }
                        j == 0 -> {
                            dp[i][j] = i
                        }
                        else -> {
                            dp[i][j] = minOf(
                                dp[i - 1][j - 1]
                                        + costOfSubstitution(x[i - 1], y[j - 1]),
                                dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1
                            )
                        }
                    }
                }
            }

            return dp[x.length][y.length]
        }

        private fun costOfSubstitution(a: Char, b: Char): Int {
            return if (a == b) 0 else 1
        }
    }
}
