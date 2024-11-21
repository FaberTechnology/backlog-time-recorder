package com.lambda.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class WorkdayUtilsTest {

    private final WorkdayUtils utils = new WorkdayUtils();

    @Test
    public void testCalculateWorkingHours_FullWeekday() {
        // Start and end on a weekday within working hours
        LocalDateTime start = LocalDateTime.of(2024, 11, 19, 10, 0);
        LocalDateTime end = LocalDateTime.of(2024, 11, 19, 18, 0);

        Duration expected = Duration.between(LocalTime.of(10, 0), LocalTime.of(18, 0));
        Duration actual = utils.calculateWorkingHours(start, end);

        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateWorkingHours_StartBeforeWorkday() {
        // Start before workday, end within workday
        LocalDateTime start = LocalDateTime.of(2024, 11, 19, 8, 0);
        LocalDateTime end = LocalDateTime.of(2024, 11, 19, 15, 0);

        Duration expected = Duration.between(LocalTime.of(8, 0), LocalTime.of(15, 0));
        Duration actual = utils.calculateWorkingHours(start, end);

        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateWorkingHours_EndAfterWorkday() {
        // Start within workday, end after workday
        LocalDateTime start = LocalDateTime.of(2024, 11, 19, 14, 0);
        LocalDateTime end = LocalDateTime.of(2024, 11, 19, 20, 0);

        Duration expected = Duration.between(LocalTime.of(14, 0), LocalTime.of(20, 0));
        Duration actual = utils.calculateWorkingHours(start, end);

        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateWorkingHours_Weekend() {
        // Start and end on a weekend
        LocalDateTime start = LocalDateTime.of(2024, 11, 16, 10, 0); // Saturday
        LocalDateTime end = LocalDateTime.of(2024, 11, 17, 18, 0); // Sunday

        Duration expected = Duration.ZERO;
        Duration actual = utils.calculateWorkingHours(start, end);

        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateWorkingHours_AcrossMultipleDays() {
        // Start on one day, end on another day
        LocalDateTime start = LocalDateTime.of(2024, 11, 18, 16, 0); // Monday
        LocalDateTime end = LocalDateTime.of(2024, 11, 19, 11, 0); // Tuesday

        Duration expected = Duration.between(LocalTime.of(16, 0), LocalTime.of(19, 30))
                .plus(Duration.between(LocalTime.of(9, 30), LocalTime.of(11, 0)));
        Duration actual = utils.calculateWorkingHours(start, end);

        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateWorkingHours_AcrossWeekend() {
        // Start on Friday, end on Monday (across weekend)
        LocalDateTime start = LocalDateTime.of(2024, 11, 15, 13, 0); // Friday
        LocalDateTime end = LocalDateTime.of(2024, 11, 18, 10, 0); // Monday

        Duration expected = Duration.ofHours(6).plusMinutes(30) // Friday
                .plus(Duration.ofHours(0).plusMinutes(30)); // Monday
        Duration actual = utils.calculateWorkingHours(start, end);

        assertEquals(expected, actual);
    }
}