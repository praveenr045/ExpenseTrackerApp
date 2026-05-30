package com.personal.expensetracker.sms

import com.personal.expensetracker.data.model.ExpenseCategory

data class ParsedSms(
    val amount: Double,
    val merchant: String,
    val category: ExpenseCategory,
    val description: String
)

object SmsParser {

    private val AMOUNT_PATTERNS = listOf(
        // INR/Rs before amount
        Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        // Amount before INR/Rs
        Regex("""([\d,]+(?:\.\d{1,2})?)\s*(?:INR|Rs\.?|₹)""", RegexOption.IGNORE_CASE),
        // "debited for/by/with amount"
        Regex("""debited\s+(?:for|by|of|with|:)?\s*(?:INR|Rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        // "amount X"
        Regex("""(?:amount|amt)[:\s]+(?:INR|Rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        // Canara style: "for Rs X"
        Regex("""for\s+(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    )

    private val MERCHANT_PATTERNS = listOf(
        Regex("""(?:at|to|@)\s+([A-Za-z0-9\s&\-\.\/]+?)(?:\s+on|\s+via|\s+for|\s+ref|\s+vpa|\.|,|${'$'})""", RegexOption.IGNORE_CASE),
        Regex("""(?:merchant|retailer)[:\s]+([A-Za-z0-9\s&\-\.]+?)(?:\s+on|\s+via|\.|,|${'$'})""", RegexOption.IGNORE_CASE),
        Regex("""UPI[:\-\s]+([A-Za-z0-9\s&\-\.@]+?)(?:\s+on|\s+Ref|\.|,|${'$'})""", RegexOption.IGNORE_CASE),
        // Canara: "transferred to NAME"
        Regex("""(?:transferred|transfer|sent)\s+to\s+([A-Za-z0-9\s&\-\.]+?)(?:\s+on|\s+via|\.|,|${'$'})""", RegexOption.IGNORE_CASE)
    )

    // Expanded debit keywords — including Canara UPI transfer patterns
    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "spent", "payment", "paid", "purchase",
        "withdraw", "withdrawn", "transaction", "txn", "pos ", "upi",
        "transferred", "transfer", "sent to", "imps", "neft", "rtgs",
        "a/c is debited", "your account", "has been debited"
    )

    // Expanded bank sender IDs — including all Canara SMS sender variants
    private val BANK_SENDERS = listOf(
        // HDFC
        "HDFCBK", "HDFCBANK", "HDFC",
        // ICICI
        "ICICI", "ICICIB", "ICICIBK",
        // Canara Bank — multiple sender IDs used
        "CNRB", "CANARA", "CANARABANK", "CBSSMS", "CANBK", "CANBNK",
        "CNBNK", "CANARAB", "CANBNKUPI", "CBSUPI", "CNRUPI",
        // Generic fallback
        "BANKUPI"
    )

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
            "bigbasket", "grofer", "zepto", "blinkit", "dmart", "reliance fresh",
            "more supermarket", "grocery", "supermarket", "vegetables", "fruits"
        ),
        ExpenseCategory.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho",
            "shopping", "mall", "store", "shop", "retail"
        ),
        ExpenseCategory.BILLS to listOf(
            "electricity", "bescom", "tneb", "water", "gas", "airtel",
            "jio", "vi ", "vodafone", "bsnl", "broadband", "wifi", "recharge",
            "bill", "utility", "insurance", "lic", "premium"
        ),
        ExpenseCategory.HEALTH to listOf(
            "pharmacy", "medical", "hospital", "clinic", "doctor", "medicine",
            "apollo", "netmeds", "1mg", "pharmeasy", "diagnostic", "lab"
        ),
        ExpenseCategory.ENTERTAINMENT to listOf(
            "netflix", "hotstar", "amazon prime", "spotify", "youtube premium",
            "movie", "theatre", "pvr", "inox", "bookmyshow", "game"
        ),
        ExpenseCategory.EDUCATION to listOf(
            "school", "college", "university", "course", "udemy", "coursera",
            "byju", "unacademy", "tuition", "fees", "exam"
        )
    )

    fun isBankSms(sender: String): Boolean {
        val upper = sender.uppercase().replace("-", "").replace("_", "")
        return BANK_SENDERS.any { upper.contains(it.replace("-", "").replace("_", "")) }
    }

    fun isDebitSms(body: String): Boolean {
        val lower = body.lowercase()
        return DEBIT_KEYWORDS.any { lower.contains(it) }
    }

    fun parse(body: String): ParsedSms? {
        val amount = extractAmount(body) ?: return null
        val merchant = extractMerchant(body)
        val category = inferCategory(body, merchant)
        val description = buildDescription(merchant, body)
        return ParsedSms(amount, merchant, category, description)
    }

    private fun extractAmount(body: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val raw = match.groupValues[1].replace(",", "")
                val value = raw.toDoubleOrNull()
                if (value != null && value > 0) return value
            }
        }
        return null
    }

    private fun extractMerchant(body: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val merchant = match.groupValues[1].trim()
                if (merchant.length in 2..50) return merchant
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

    private fun buildDescription(merchant: String, body: String): String {
        if (merchant.isNotBlank()) return "Payment at $merchant"
        return body.take(60).trimEnd()
    }
}