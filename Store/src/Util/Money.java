package Util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Decimal arithmetic; round monetary inputs once to the currency's two decimals. */
public final class Money {
    private Money() { }
    public static BigDecimal of(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Money must be finite.");
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
    public static BigDecimal of(String value) {
        try { return new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException | NumberFormatException e) { throw new IllegalArgumentException("Enter a price with at most two decimal places."); }
    }
    public static long cents(double value) { return of(value).movePointRight(2).longValueExact(); }
    public static double fromCents(long value) { return BigDecimal.valueOf(value, 2).doubleValue(); }
    public static String format(BigDecimal value) { return "$" + value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
}
