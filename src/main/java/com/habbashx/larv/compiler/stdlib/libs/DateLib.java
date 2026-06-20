package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class DateLib implements LarvStdlib {

    @Contract(pure = true)
    @Override public @NotNull String name() { return "date"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("now",        "date_now"),
                Map.entry("today",      "date_today"),
                Map.entry("year",       "date_year"),
                Map.entry("month",      "date_month"),
                Map.entry("day",        "date_day"),
                Map.entry("hour",       "date_hour"),
                Map.entry("minute",     "date_minute"),
                Map.entry("second",     "date_second"),
                Map.entry("formatDate", "date_formatDate"),
                Map.entry("parseDate",  "date_parseDate"),
                Map.entry("dateDiff",   "date_dateDiff"),
                Map.entry("addDays",    "date_addDays"),
                Map.entry("addMonths",  "date_addMonths"),
                Map.entry("dayOfWeek",  "date_dayOfWeek"),
                Map.entry("isLeapYear", "date_isLeapYear")
        );
    }

    public static Object date_now()     { return (double) System.currentTimeMillis(); }
    public static @NotNull @Unmodifiable Object date_today()   { return LocalDate.now().toString(); }
    public static Object date_year()    { return (double) LocalDate.now().getYear(); }
    public static Object date_month()   { return (double) LocalDate.now().getMonthValue(); }
    public static Object date_day()     { return (double) LocalDate.now().getDayOfMonth(); }
    public static Object date_hour()    { return (double) LocalTime.now().getHour(); }
    public static Object date_minute()  { return (double) LocalTime.now().getMinute(); }
    public static Object date_second()  { return (double) LocalTime.now().getSecond(); }

    public static @NotNull @Unmodifiable Object date_formatDate(Object millis, Object pattern) {
        long ms  = (long) MathLib.num(millis, "formatDate");
        String fmt = pattern instanceof String s ? s : "yyyy-MM-dd HH:mm:ss";
        return DateTimeFormatter.ofPattern(fmt)
                .format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()));
    }

    public static Object date_parseDate(Object dateStr, Object pattern) {
        String s   = str(dateStr, "parseDate");
        String fmt = pattern instanceof String p ? p : "yyyy-MM-dd";
        try {
            LocalDate d = LocalDate.parse(s, DateTimeFormatter.ofPattern(fmt));
            return (double) d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) { throw new LarvRuntimeException("parseDate(): " + e.getMessage()); }
    }

    public static Object date_dateDiff(Object millis1, Object millis2) {
        return (double) Math.abs((long) MathLib.num(millis2, "dateDiff") - (long) MathLib.num(millis1, "dateDiff"));
    }

    public static Object date_addDays(Object millis, Object days) {
        long ms = (long) MathLib.num(millis, "addDays");
        int  d  = (int)  MathLib.num(days,   "addDays");
        return (double) Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
                .plusDays(d).toInstant().toEpochMilli();
    }

    public static Object date_addMonths(Object millis, Object months) {
        long ms = (long) MathLib.num(millis, "addMonths");
        int  m  = (int)  MathLib.num(months, "addMonths");
        return (double) Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
                .plusMonths(m).toInstant().toEpochMilli();
    }

    public static @NotNull @Unmodifiable Object date_dayOfWeek()  { return LocalDate.now().getDayOfWeek().name(); }
    public static @NotNull @Unmodifiable Object date_isLeapYear() { return LocalDate.now().isLeapYear(); }

    @Contract("null, _ -> fail")
    private static String str(Object v, String fn) {
        if (!(v instanceof String s)) throw new LarvRuntimeException(fn + "(): argument must be a string");
        return s;
    }
}