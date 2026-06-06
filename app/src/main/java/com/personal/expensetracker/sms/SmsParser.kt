package com.personal.expensetracker.sms

import com.personal.expensetracker.data.model.ExpenseCategory

data class ParsedSms(
    val amount: Double,
    val merchant: String,
    val category: ExpenseCategory,
    val description: String
)

object SmsParser {

    // ── Amount patterns ───────────────────────────────────────────────────────
    private val AMOUNT_PATTERNS = listOf(
        Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""([\d,]+(?:\.\d{1,2})?)\s*(?:INR|Rs\.?|₹)""", RegexOption.IGNORE_CASE),
        Regex("""debited\s+for\s+(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""debited\s+(?:for|by|of|with|:)?\s*(?:INR|Rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:amount|amt)[:\s]+(?:INR|Rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""for\s+(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    )

    // ── Merchant / payee patterns — ordered from most specific to least ───────
    private val MERCHANT_PATTERNS = listOf(

        // ICICI specific: "KAVYA R M credited" — name before "credited"
        Regex(""";\s*([A-Z][A-Za-z\s\.]+?)\s+credited"""),

        // "transferred to NAME" (Canara UPI peer transfer)
        Regex("""(?:transferred|transfer|sent)\s+to\s+([A-Za-z0-9\s&\-\.]+?)(?:\s+on|\s+via|\s+ref|\.|,|${'$'})""", RegexOption.IGNORE_CASE),

        // "at MERCHANT on" / "to MERCHANT on"
        Regex("""(?:at|to|@)\s+([A-Za-z0-9\s&\-\.\/]+?)(?:\s+on|\s+via|\s+for|\s+ref|\s+vpa|\.|,|${'$'})""", RegexOption.IGNORE_CASE),

        // "merchant: NAME"
        Regex("""(?:merchant|retailer)[:\s]+([A-Za-z0-9\s&\-\.]+?)(?:\s+on|\s+via|\.|,|${'$'})""", RegexOption.IGNORE_CASE),

        // "UPI: VPA" or "UPI/VPA"
        Regex("""UPI[:\-\/\s]+([A-Za-z0-9\s&\-\.@]+?)(?:\s+on|\s+Ref|\.|,|${'$'})""", RegexOption.IGNORE_CASE)
    )

    // ── Debit keywords ────────────────────────────────────────────────────────
    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "spent", "payment", "paid", "purchase",
        "withdraw", "withdrawn", "transaction", "txn", "pos ", "upi",
        "transferred", "transfer", "sent to", "imps", "neft", "rtgs",
        "a/c is debited", "your account", "has been debited"
    )

    // ── Bank sender IDs ───────────────────────────────────────────────────────
    private val BANK_SENDERS = listOf(
        // HDFC
        "HDFCBK", "HDFCBANK", "HDFC",
        // ICICI
        "ICICI", "ICICIB", "ICICIBK",
        // Canara
        "CNRB", "CANARA", "CANARABANK", "CBSSMS", "CANBK", "CANBNK",
        "CNBNK", "CANARAB", "CANBNKUPI", "CBSUPI", "CNRUPI"
    )

    // ── Category keywords ─────────────────────────────────────────────────────
    private val CATEGORY_KEYWORDS = mapOf(
        ExpenseCategory.FOOD to listOf(
            "zomato", "swiggy", "restaurant", "cafe", "food", "pizza",
            "burger", "hotel", "dhaba", "biryani", "dominos", "mcdonalds", "kfc"
        ),
        ExpenseCategory.TRANSPORT to listOf(
            "uber", "ola", "rapido", "metro", "bus", "cab", "auto",
            "taxi", "irctc", "train", "flight", "redbus", "makemytrip"
        ),
        ExpenseCategory.FUEL to listOf(
            "petrol", "diesel", "fuel", "hp ", "bharat petroleum",
            "indian oil", "iocl", "hpcl", "bpcl", "shell"
        ),
        ExpenseCategory.GROCERIES to listOf(
            "bigbasket", "grofer", "zepto", "blinkit", "dmart",
            "reliance fresh", "more supermarket", "grocery", "supermarket",
            "vegetables", "fruits"
        ),
        ExpenseCategory.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho",
            "shopping", "mall", "store", "shop", "retail"
        ),
        ExpenseCategory.BILLS to listOf(
            "electricity", "bescom", "tneb", "water", "gas", "airtel",
            "jio", "vi ", "vodafone", "bsnl", "broadband", "wifi",
            "recharge", "bill", "utility", "insurance", "lic", "premium"
        ),
        ExpenseCategory.HEALTH to listOf(
            "pharmacy", "medical", "hospital", "clinic", "doctor",
            "medicine", "apollo", "netmeds", "1mg", "pharmeasy",
            "diagnostic", "lab"
        ),
        ExpenseCategory.ENTERTAINMENT to listOf(
            "netflix", "hotstar", "amazon prime", "spotify",
            "youtube premium", "movie", "theatre", "pvr", "inox",
            "bookmyshow", "game"
        ),
        ExpenseCategory.EDUCATION to listOf(
            "school", "college", "university", "course", "udemy",
            "coursera", "byju", "unacademy", "tuition", "fees", "exam"
        )
    )

    // ─────────────────────────────────────────────────────────────────────────

    fun isBankSms(sender: String): Boolean {
        val clean = sender.uppercase().replace("-", "").replace("_", "")
        return BANK_SENDERS.any { clean.contains(it.replace("-", "")) }
    }

    fun isDebitSms(body: String): Boolean {
        val lower = body.lowercase()
        return DEBIT_KEYWORDS.any { lower.contains(it) }
    }

    fun parse(body: String): ParsedSms? {
        val amount   = extractAmount(body)   ?: return null
        val merchant = extractMerchant(body)
        val category = inferCategory(body, merchant)
        val description = buildDescription(merchant, body)
        return ParsedSms(amount, merchant, category, description)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun extractAmount(body: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(body) ?: continue
            val raw   = match.groupValues[1].replace(",", "")
            val value = raw.toDoubleOrNull()
            if (value != null && value > 0) return value
        }
        return null
    }

    private fun extractMerchant(body: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            val match    = pattern.find(body) ?: continue
            val merchant = match.groupValues[1].trim()
            // Must be 2–50 chars, not a pure number, not a phone number
            if (merchant.length in 2..50 &&
                !merchant.matches(Regex("""[\d\s]+""")) &&
                !merchant.startsWith("18") // skip toll-free numbers
            ) {
                return merchant.capitalizeWords()
            }
        }
        return ""
    }

    private fun inferCategory(body: String, merchant: String): ExpenseCategory {
        val text = (body + " " + merchant).lowercase()
        for ((category, keywords) in CATEGORY_KEYWORDS) {
            if (keywords.any { text.contains(it) }) return category
        }
        return ExpenseCategory.OTHER
    }

    private fun buildDescription(merchant: String, body: String): String =
        if (merchant.isNotBlank()) "Paid to $merchant"
        else body.take(60).trimEnd()

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { word ->
            if (word.length <= 2) word.uppercase()   // initials like "R", "M"
            else word.replaceFirstChar { it.uppercase() }
        }
}