package com.chronicle.expense.sms

import com.chronicle.expense.data.Money

/**
 * Turns a sample phrase such as `BDT 9,203.00 purchased` into a matcher that pulls the
 * amount out of any message containing the same words around a (different) number.
 */
class SmsPattern private constructor(private val regex: Regex) {

    /** Amount in minor units, or null when the message does not match. */
    fun extract(body: String): Long? {
        val match = regex.find(body) ?: return null
        return Money.parse(match.groupValues[1])?.takeIf { it > 0 }
    }

    companion object {
        private const val NUMBER = """\d[\d,]*(?:\.\d+)?"""
        private val numberRegex = Regex(NUMBER)

        /** Null when the sample has no number or no surrounding words to anchor on. */
        fun compile(sample: String): SmsPattern? {
            val text = sample.trim()
            val number = numberRegex.find(text) ?: return null
            val before = literal(text.substring(0, number.range.first))
            val after = literal(text.substring(number.range.last + 1))
            if (before.isEmpty() && after.isEmpty()) return null

            val pattern = buildString {
                if (before.isNotEmpty()) append(before).append("""\s*""")
                append("(").append(NUMBER).append(")")
                if (after.isNotEmpty()) append("""\s*""").append(after)
            }
            return SmsPattern(Regex(pattern, RegexOption.IGNORE_CASE))
        }

        /** Escapes each word and lets any run of whitespace sit between them. */
        private fun literal(part: String): String =
            part.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
                .joinToString("""\s+""") { Regex.escape(it) }

        fun senderMatches(ruleSender: String, actual: String): Boolean {
            val a = normalize(ruleSender)
            val b = normalize(actual)
            return a.isNotEmpty() && (b == a || b.contains(a))
        }

        private fun normalize(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
    }
}
