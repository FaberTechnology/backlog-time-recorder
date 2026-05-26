package com.lambda.helpers;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

public class WorkScheduleHelper {

    private static final List<DayOfWeek> WEEKENDS = Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 30);
    private static final LocalTime WORK_END_TIME = LocalTime.of(19, 30);

    public Duration calculateWorkingHours(final LocalDateTime start, final LocalDateTime end) {
        LocalDateTime current = start;
        Duration totalWorkingDuration = Duration.ZERO;

        while (current.isBefore(end)) {
            if (isWorkingDay(current.toLocalDate())) {
                LocalDateTime endOfDay = current.toLocalDate().atTime(WORK_END_TIME);
                LocalDateTime actualEnd = isSameDate(end, endOfDay) ? end : endOfDay;
                totalWorkingDuration = totalWorkingDuration.plus(Duration.between(current, actualEnd));
            }
            current = current.plusDays(1).toLocalDate().atTime(WORK_START_TIME);
        }

        return totalWorkingDuration;
    }

    public boolean isWorkingDay(final LocalDate date) {
        return !WEEKENDS.contains(date.getDayOfWeek());
    }

    private boolean isSameDate(final LocalDateTime dateTime1, final LocalDateTime dateTime2) {
        return dateTime1.toLocalDate().equals(dateTime2.toLocalDate());
    }
}
