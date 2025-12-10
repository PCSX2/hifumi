package net.pcsx2.hifumi.util;

import java.time.Clock;
import java.time.OffsetDateTime;

public class TimeUtils {

    /**
     * Get a pretty epoch second value going back length days in time, and then flushing out to the nearest timeUnit.
     * @param timeUnit
     * @param length
     * @return
     */
    public static long getEpochSecondStartOfUnit(String timeUnit, long length) {
        OffsetDateTime daysSubtracted = OffsetDateTime.now(Clock.systemUTC()).minusDays(length);
        OffsetDateTime adjustedToStart = daysSubtracted;

        if (timeUnit.equals("Months")) {
            adjustedToStart = (daysSubtracted.getDayOfMonth() > 1 ? daysSubtracted.minusDays(daysSubtracted.getDayOfMonth() - 1) : daysSubtracted);
        }
        
        OffsetDateTime toMidnight = adjustedToStart.minusHours(adjustedToStart.getHour()).minusMinutes(adjustedToStart.getMinute()).minusSeconds(adjustedToStart.getSecond()).minusNanos(adjustedToStart.getNano());
        long epochSeconds = toMidnight.toEpochSecond();
        return epochSeconds;
    }

    public static String getSQLFormatStringFromTimeUnit(String timeUnit) {
        switch (timeUnit) {
            case "year": {
                return "%Y";
            }
            case "month": {
                return "%Y-%m";
            }
            case "day": {
                return "%Y-%m-%d";
            }
            default: {
                return "%Y-%m-%d";
            }
        }
    }
}
