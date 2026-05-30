package com.personal.expensetracker.voice

import com.personal.expensetracker.data.model.ExpenseCategory

data class ParsedVoice(
    val amount: Double,
    val description: String,
    val category: ExpenseCategory
)

object VoiceParser {

    // Matches: "spent 200 on auto", "200 rupees for petrol", "paid 150 to swiggy"
    private val AMOUNT_PATTERNS = listOf(
        Regex("""(?:spent|paid|pay|spend|bought|purchased)?\s*([\d,]+(?:\.\d{1,2})?)\s*(?:rupees?|rs\.?|inr)?""", RegexOption.IGNORE_CASE),
        Regex("""([\d,]+(?:\.\d{1,2})?)\s*(?:rupees?|rs\.?|inr)""", RegexOption.IGNORE_CASE)
    )

    private val IGNORE_WORDS = setOf(
        "spent", "paid", "pay", "spend", "bought", "purchased",
        "on", "for", "to", "at", "rupees", "rupee", "rs", "inr", "a", "the"
    )

    private val CATEGORY_KEYWORDS = mapOf(
        ExpenseCategory.FOOD to listOf("food", "lunch", "dinner", "breakfast", "snack", "chai", "coffee", "eat", "restaurant", "zomato", "swiggy"),
        ExpenseCategory.TRANSPORT to listOf("auto", "cab", "uber", "ola", "bus", "metro", "train", "travel", "taxi", "rapido", "rickshaw"),
        ExpenseCategory.FUEL to listOf("petrol", "diesel", "fuel", "gas"),
        ExpenseCategory.GROCERIES to listOf("grocery", "groceries", "vegetables", "milk", "fruits", "sabji", "kirana"),
        ExpenseCategory.SHOPPING to listOf("shopping", "clothes", "shirt", "shoes", "amazon", "flipkart", "bought"),
        ExpenseCategory.BILLS to listOf("bill", "electricity", "recharge", "wifi", "internet", "phone", "mobile"),
        ExpenseCategory.HEALTH to listOf("medicine", "doctor", "hospital", "pharmacy", "medical", "tablet", "injection"),
        ExpenseCategory.ENTERTAINMENT to listOf("movie", "netflix", "hotstar", "game", "cinema", "theatre", "concert")
    )

    fun parse(transcript: String): ParsedVoice? {
        val amount = extractAmount(transcript) ?: return null
        val category = inferCategory(transcript)
        val description = buildDescription(transcript, amount)
        return ParsedVoice(amount, description, category)
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val raw = match.groupValues[1].replace(",", "")
                val value = raw.toDoubleOrNull()
                if (value != null && value > 0) return value
            }
        }
        return null
    }

    private fun inferCategory(text: String): ExpenseCategory {
        val lower = text.lowercase()
        for ((category, keywords) in CATEGORY_KEYWORDS) {
            if (keywords.any { lower.contains(it) }) return category
        }
        return ExpenseCategory.OTHER
    }

    private fun buildDescription(text: String, amount: Double): String {
        // Remove amount and filler words to get the meaningful part
        val words = text.lowercase()
            .replace(Regex("""[\d,]+(?:\.\d{1,2})?"""), "")
            .split(" ")
            .filter { it.isNotBlank() && it !in IGNORE_WORDS }
        val meaningful = words.joinToString(" ").trim().replaceFirstChar { it.uppercase() }
        return if (meaningful.isNotBlank()) meaningful else "Voice expense"
    }
}