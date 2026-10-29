package com.cashq.core.formatting

/**
 * Formatter for Indian Rupee currency following the Indian numbering system
 * (rightmost group of 3 digits, followed by groups of 2 digits, e.g., ₹1,23,456 or ₹1,23,45,678).
 */
object CurrencyFormatter {

    /**
     * Formats a raw number into the Indian numbering grouping format.
     * Examples:
     *   0 -> "0"
     *   500 -> "500"
     *   2800 -> "2,800"
     *   37000 -> "37,000"
     *   123456 -> "1,23,456"
     *   12345678 -> "1,23,45,678"
     */
    fun formatIndianNumber(number: Long): String {
        if (number < 0) return "-" + formatIndianNumber(-number)
        val str = number.toString()
        if (str.length <= 3) return str

        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)

        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i > 0) {
                sb.append(',')
            }
        }
        return sb.reverse().toString() + "," + lastThree
    }

    /**
     * Formats a whole Rupee amount into Indian grouping format with the ₹ prefix.
     * Example: 123456L -> "₹1,23,456"
     * Example: 0L -> "₹0"
     */
    fun formatRupees(amountInRupees: Long): String {
        return "₹${formatIndianNumber(amountInRupees)}"
    }

    /**
     * Formats paise into Rupees with the ₹ prefix.
     * Example: 12345600L -> "₹1,23,456"
     */
    fun formatPaise(amountInPaise: Long): String {
        return formatRupees(amountInPaise / 100L)
    }

    /**
     * Formats counted units (notes or coins) with Indian grouping.
     * Example: 1234L -> "1,234"
     */
    fun formatUnits(units: Long): String {
        return formatIndianNumber(units)
    }
}
