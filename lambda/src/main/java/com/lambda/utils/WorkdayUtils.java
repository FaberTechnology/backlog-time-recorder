package com.lambda.utils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

public class WorkdayUtils {
    
    private static final List<DayOfWeek> WEEKENDS = Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 30); // Start at 09:30 JST
    private static final LocalTime WORK_END_TIME = LocalTime.of(19, 30); // End at 17:30 ICT as 19:30 JST
    
    public Duration calculateWorkingHours(final LocalDateTime start, final LocalDateTime end) {
        LocalDateTime current = start;
        Duration totalWorkingDuration = Duration.ZERO;

        while (current.isBefore(end)) {
            if (isWeekday(current)) {
                LocalDateTime endOfDay = current.toLocalDate().atTime(WORK_END_TIME); 
                
                // Add hours for current day or until end time
                LocalDateTime actualEnd = isSameDate(end, endOfDay) ? end : endOfDay;
                totalWorkingDuration = totalWorkingDuration.plus(Duration.between(current, actualEnd));
            }

            // Move to the next day
            current = current.plusDays(1).toLocalDate().atTime(WORK_START_TIME);
        }

        return totalWorkingDuration;
    }

    private boolean isWeekday(final LocalDateTime dateTime) {
        return !WEEKENDS.contains(dateTime.getDayOfWeek());
    }

    private boolean isSameDate(final LocalDateTime dateTime1, final LocalDateTime dateTime2) {
        return dateTime1.toLocalDate().equals(dateTime2.toLocalDate());
    }
}
