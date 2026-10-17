package com.abchr.platform.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class AmountInWords {

    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen",
        "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty",
        "Ninety"};

    private AmountInWords() {
    }

    static String rupees(BigDecimal amount) {
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = scaled.longValue();
        int paise = scaled.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        StringBuilder text = new StringBuilder("Rupees ").append(rupees == 0 ? "Zero" : words(rupees));
        if (paise > 0) {
            text.append(" and ").append(words(paise)).append(" Paise");
        }
        return text.append(" Only").toString();
    }

    private static String words(long n) {
        StringBuilder out = new StringBuilder();
        long crore = n / 10_000_000;
        n %= 10_000_000;
        long lakh = n / 100_000;
        n %= 100_000;
        long thousand = n / 1000;
        n %= 1000;
        long hundred = n / 100;
        long rest = n % 100;
        if (crore > 0) {
            out.append(words(crore)).append(" Crore ");
        }
        if (lakh > 0) {
            out.append(below100(lakh)).append(" Lakh ");
        }
        if (thousand > 0) {
            out.append(below100(thousand)).append(" Thousand ");
        }
        if (hundred > 0) {
            out.append(ONES[(int) hundred]).append(" Hundred ");
        }
        if (rest > 0) {
            out.append(below100(rest));
        }
        return out.toString().trim();
    }

    private static String below100(long n) {
        if (n < 20) {
            return ONES[(int) n];
        }
        return (TENS[(int) (n / 10)] + " " + ONES[(int) (n % 10)]).trim();
    }
}
