package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Date/Time standard library — import "date"
 *
 * All timestamps are Unix epoch seconds (number) unless noted.
 *
 *   timestamp()                    → number   current Unix epoch seconds
 *   dateNow()                      → string   current date "yyyy-MM-dd"
 *   timeNow()                      → string   current time "HH:mm:ss"
 *   dateTimeNow()                  → string   current datetime "yyyy-MM-dd HH:mm:ss"
 *   dateFormat(ts, pattern)        → string   format timestamp with pattern
 *   dateParse(str, pattern)        → number   parse date string → timestamp
 *   dateAdd(ts, amount, unit)      → number   add amount of unit to timestamp
 *                                             unit: "seconds" "minutes" "hours" "days" "weeks" "months" "years"
 *   dateSub(ts, amount, unit)      → number   subtract amount of unit from timestamp
 *   dateDiff(ts1, ts2)             → number   difference in days (ts2 - ts1)
 *   dayOfWeek(ts)                  → string   e.g. "Monday"
 *   monthName(ts)                  → string   e.g. "January"
 *   year(ts)                       → number   year component
 *   month(ts)                      → number   month component (1-12)
 *   day(ts)                        → number   day-of-month component
 *   hour(ts)                       → number   hour component (0-23)
 *   minute(ts)                     → number   minute component (0-59)
 *   second(ts)                     → number   second component (0-59)
 *   isBefore(ts1, ts2)             → boolean  ts1 < ts2
 *   isAfter(ts1, ts2)              → boolean  ts1 > ts2
 */
@Native("Date Library")
public class NativeDateLibrary implements NativeLibrary {

    private final ExecutionContext context;

    public NativeDateLibrary(ExecutionContext context) {
        this.context = context;
    }

    @Override
    public void registerAll() {
        context.registerNative("timestamp",   this::timestamp);
        context.registerNative("dateNow",     this::dateNow);
        context.registerNative("timeNow",     this::timeNow);
        context.registerNative("dateTimeNow", this::dateTimeNow);
        context.registerNative("dateFormat",  this::dateFormat);
        context.registerNative("dateParse",   this::dateParse);
        context.registerNative("dateAdd",     this::dateAdd);
        context.registerNative("dateSub",     this::dateSub);
        context.registerNative("dateDiff",    this::dateDiff);
        context.registerNative("dayOfWeek",   this::dayOfWeek);
        context.registerNative("monthName",   this::monthName);
        context.registerNative("year",        this::year);
        context.registerNative("month",       this::month);
        context.registerNative("day",         this::day);
        context.registerNative("hour",        this::hour);
        context.registerNative("minute",      this::minute);
        context.registerNative("second",      this::second);
        context.registerNative("isBefore",    this::isBefore);
        context.registerNative("isAfter",     this::isAfter);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private double numArg(@NotNull List<Object> args, int i, String fn) {
        if (args.size() <= i || !(args.get(i) instanceof Double d))
            throw new LarvError(fn + "(): argument " + (i + 1) + " must be a number (timestamp)", -1, LarvError.Kind.RUNTIME);
        return (Double) args.get(i);
    }

    private String strArg(@NotNull List<Object> args, int i, String fn) {
        if (args.size() <= i || !(args.get(i) instanceof String s))
            throw new LarvError(fn + "(): argument " + (i + 1) + " must be a string", -1, LarvError.Kind.RUNTIME);
        return s;
    }

    @Contract("_ -> new")
    private @NotNull LocalDateTime fromTs(double ts) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond((long) ts), ZoneId.systemDefault());
    }

    private double toTs(@NotNull LocalDateTime dt) {
        return (double) dt.toInstant(ZoneId.systemDefault().getRules().getOffset(dt)).getEpochSecond();
    }


    private Object timestamp(List<Object> args) {
        return (double) Instant.now().getEpochSecond();
    }

    private @NotNull @Unmodifiable Object dateNow(List<Object> args) {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private @NotNull @Unmodifiable Object timeNow(List<Object> args) {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private @NotNull @Unmodifiable Object dateTimeNow(List<Object> args) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private @NotNull @Unmodifiable Object dateFormat(@NotNull List<Object> args) {
        double ts      = numArg(args, 0, "dateFormat");
        String pattern = strArg(args, 1, "dateFormat");
        try {
            return fromTs(ts).format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            throw new LarvError("dateFormat(): invalid pattern '" + pattern + "': " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @NotNull @Unmodifiable Object dateParse(@NotNull List<Object> args) {
        String dateStr = strArg(args, 0, "dateParse");
        String pattern = strArg(args, 1, "dateParse");
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
            try {
                return toTs(LocalDateTime.parse(dateStr, fmt));
            } catch (Exception ignored) {
                LocalDate d = LocalDate.parse(dateStr, fmt);
                return toTs(d.atStartOfDay());
            }
        } catch (Exception e) {
            throw new LarvError("dateParse(): cannot parse '" + dateStr + "' with pattern '" + pattern + "'", -1, LarvError.Kind.RUNTIME);
        }
    }

    private @NotNull @Unmodifiable Object dateAdd(@NotNull List<Object> args) {
        double ts     = numArg(args, 0, "dateAdd");
        double amount = numArg(args, 1, "dateAdd");
        String unit   = strArg(args, 2, "dateAdd");
        return toTs(applyUnit(fromTs(ts), (long) amount, unit, "dateAdd"));
    }

    private @NotNull @Unmodifiable Object dateSub(@NotNull List<Object> args) {
        double ts     = numArg(args, 0, "dateSub");
        double amount = numArg(args, 1, "dateSub");
        String unit   = strArg(args, 2, "dateSub");
        return toTs(applyUnit(fromTs(ts), -(long) amount, unit, "dateSub"));
    }

    private LocalDateTime applyUnit(LocalDateTime dt, long amount, @NotNull String unit, String fn) {
        return switch (unit) {
            case "seconds" -> dt.plusSeconds(amount);
            case "minutes" -> dt.plusMinutes(amount);
            case "hours"   -> dt.plusHours(amount);
            case "days"    -> dt.plusDays(amount);
            case "weeks"   -> dt.plusWeeks(amount);
            case "months"  -> dt.plusMonths(amount);
            case "years"   -> dt.plusYears(amount);
            default -> throw new LarvError(fn + "(): unknown unit '" + unit +
                    "'. Use: seconds, minutes, hours, days, weeks, months, years", -1, LarvError.Kind.RUNTIME);
        };
    }

    private Object dateDiff(@NotNull List<Object> args) {
        double ts1 = numArg(args, 0, "dateDiff");
        double ts2 = numArg(args, 1, "dateDiff");
        long seconds = (long) ts2 - (long) ts1;
        return (double) (seconds / 86400);
    }

    private @NotNull @Unmodifiable Object dayOfWeek(@NotNull List<Object> args) {
        return fromTs(numArg(args, 0, "dayOfWeek"))
                .getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private @NotNull @Unmodifiable Object monthName(@NotNull List<Object> args) {
        return fromTs(numArg(args, 0, "monthName"))
                .getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private Object year(List<Object> args)   { return (double) fromTs(numArg(args, 0, "year")).getYear(); }
    private Object month(List<Object> args)  { return (double) fromTs(numArg(args, 0, "month")).getMonthValue(); }
    private Object day(List<Object> args)    { return (double) fromTs(numArg(args, 0, "day")).getDayOfMonth(); }
    private Object hour(List<Object> args)   { return (double) fromTs(numArg(args, 0, "hour")).getHour(); }
    private Object minute(List<Object> args) { return (double) fromTs(numArg(args, 0, "minute")).getMinute(); }
    private Object second(List<Object> args) { return (double) fromTs(numArg(args, 0, "second")).getSecond(); }

    private @NotNull Object isBefore(@NotNull List<Object> args) {
        return numArg(args, 0, "isBefore") < numArg(args, 1, "isBefore");
    }

    private @NotNull Object isAfter(@NotNull List<Object> args) {
        return numArg(args, 0, "isAfter") > numArg(args, 1, "isAfter");
    }
}