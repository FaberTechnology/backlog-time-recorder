package com.lambda.helpers;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimeTrackingHelper {

    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");
    private final WorkScheduleHelper workScheduleHelper;

    public TimeTrackingHelper(final WorkScheduleHelper workScheduleHelper) {
        this.workScheduleHelper = workScheduleHelper;
    }

    public Float calculateActualHours(final LocalDateTime createdAt, final String startedAtValue) {
        if (startedAtValue != null && !startedAtValue.isBlank()) {
            try {
                final String[] parts = startedAtValue.split(";");
                final List<String> times = new ArrayList<>(Arrays.asList(parts));
                times.add(LocalDateTime.ofInstant(Instant.now(), JST_ZONE).toString());

                java.time.Duration elapsed = java.time.Duration.ZERO;
                for (int i = 0; i < times.size() - 1; i += 2) {
                    final LocalDateTime start = LocalDateTime.parse(times.get(i));
                    final LocalDateTime end = LocalDateTime.parse(times.get(i + 1));
                    elapsed = elapsed.plus(workScheduleHelper.calculateWorkingHours(start, end));
                }

                if (!elapsed.isZero()) {
                    return roundToOneDecimal(elapsed.toMinutes() / 60.0f);
                }
            } catch (DateTimeParseException e) {
                // fall through to creation-based calculation
            }
        }

        final java.time.Duration elapsed = workScheduleHelper.calculateWorkingHours(createdAt, LocalDateTime.now());
        return roundToOneDecimal(elapsed.toMinutes() / 60.0f);
    }

    public String formatStartedAt(final String existingValue) {
        final String now = LocalDateTime.ofInstant(Instant.now(), JST_ZONE).toString();
        if (existingValue != null && !existingValue.isBlank()) {
            return existingValue + ";" + now;
        }
        return now;
    }

    private float roundToOneDecimal(final float value) {
        final DecimalFormat df = new DecimalFormat("0.0");
        return Float.parseFloat(df.format(value));
    }
}
