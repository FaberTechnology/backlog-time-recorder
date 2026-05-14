package com.lambda.helpers;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TimeTrackingHelper {

    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");

    private final WorkScheduleHelper workScheduleHelper;

    public TimeTrackingHelper(WorkScheduleHelper workScheduleHelper) {
        this.workScheduleHelper = workScheduleHelper;
    }

    public Duration calculateActualHours(String startedAtValue) {
        Duration elapsed = Duration.ZERO;
        try {
            String[] startAtArray = startedAtValue.split(";");
            List<String> timesList = new ArrayList<>(Arrays.asList(startAtArray));
            timesList.add(LocalDateTime.ofInstant(Instant.now(), JST_ZONE).toString());

            for (int i = 0; i < timesList.size() - 1; i += 2) {
                LocalDateTime startAt = LocalDateTime.parse(timesList.get(i));
                LocalDateTime endAt = LocalDateTime.parse(timesList.get(i + 1));
                elapsed = elapsed.plus(workScheduleHelper.calculateWorkingHours(startAt, endAt));
            }
        } catch (DateTimeParseException ex) {
            // preserve existing behavior
        }
        return elapsed;
    }

    public Duration calculateFallbackHours(Date issueCreated) {
        LocalDateTime start = LocalDateTime.ofInstant(
                issueCreated.toInstant(), ZoneId.systemDefault());
        return workScheduleHelper.calculateWorkingHours(start, LocalDateTime.now());
    }

    public String appendTimestamp(String existingValue) {
        String now = LocalDateTime.ofInstant(Instant.now(), JST_ZONE).toString();
        if (existingValue != null && !existingValue.isBlank()) {
            return existingValue + ";" + now;
        }
        return now;
    }

    public Float formatActualHours(Duration elapsed) {
        DecimalFormat df = new DecimalFormat("0.0");
        float hours = Float.parseFloat(df.format(elapsed.toMinutes() / 60.0));
        if (hours > 999 || hours < 0) {
            return null;
        }
        return hours;
    }
}
