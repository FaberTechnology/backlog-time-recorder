package com.lambda.helpers;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WorkScheduleHelperTest {

    private final WorkScheduleHelper helper = new WorkScheduleHelper();

    @Test
    public void testCalculateWorkingHours_FullWeekday() {
        LocalDateTime start = LocalDateTime.of(2024, 11, 19, 10, 0);
        LocalDateTime end = LocalDateTime.of(2024, 11, 19, 18, 0);

        Duration expected = Duration.between(LocalTime.of(10, 0), LocalTime.of(18, 0));
        Duration actual = helper.calculateWorkingHours(start, end);

        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateWorkingHours_StartBeforeWorkday() {
        LocalDateTime start = LocalDateTime.of(2024, 11, 19, 8, 0);
        LocalDateTime end = LocalDateTime.of(2024, 11, 19, 15, 0);

        Duration expected = Duration.between(LocalTime.of(8, 0), LocalTime.of(15, 0));
        Duration actual = helper.calculateWorkingHours(start, end);

        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateWorkingHours_EndAfterWorkday() {
        LocalDateTime start = LocalDateTime.of(2024, 11, 19, 14, 0);
        LocalDateTime end = LocalDateTime.of(2024, 11, 19, 20, 0);

        Duration expected = Duration.between(LocalTime.of(14, 0), LocalTime.of(20, 0));
        Duration actual = helper.calculateWorkingHours(start, end);

        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateWorkingHours_Weekend() {
        LocalDateTime start = LocalDateTime.of(2024, 11, 16, 10, 0); // Saturday
        LocalDateTime end = LocalDateTime.of(2024, 11, 17, 18, 0);   // Sunday

        Duration expected = Duration.ZERO;
        Duration actual = helper.calculateWorkingHours(start, end);

        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateWorkingHours_AcrossMultipleDays() {
        LocalDateTime start = LocalDateTime.of(2024, 11, 18, 16, 0); // Monday
        LocalDateTime end = LocalDateTime.of(2024, 11, 19, 11, 0);   // Tuesday

        Duration expected = Duration.between(LocalTime.of(16, 0), LocalTime.of(19, 30))
                .plus(Duration.between(LocalTime.of(9, 30), LocalTime.of(11, 0)));
        Duration actual = helper.calculateWorkingHours(start, end);

        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateWorkingHours_AcrossWeekend() {
        LocalDateTime start = LocalDateTime.of(2024, 11, 15, 13, 0); // Friday
        LocalDateTime end = LocalDateTime.of(2024, 11, 18, 10, 0);   // Monday

        Duration expected = Duration.ofHours(6).plusMinutes(30)
                .plus(Duration.ofHours(0).plusMinutes(30));
        Duration actual = helper.calculateWorkingHours(start, end);

        assertEquals(expected, actual);
    }

    @Test
    public void testIsWorkingDay_Weekday() {
        assertTrue(helper.isWorkingDay(LocalDate.of(2024, 11, 18))); // Monday
        assertTrue(helper.isWorkingDay(LocalDate.of(2024, 11, 19))); // Tuesday
        assertTrue(helper.isWorkingDay(LocalDate.of(2024, 11, 22))); // Friday
    }

    @Test
    public void testIsWorkingDay_Weekend() {
        assertFalse(helper.isWorkingDay(LocalDate.of(2024, 11, 16))); // Saturday
        assertFalse(helper.isWorkingDay(LocalDate.of(2024, 11, 17))); // Sunday
    }

    @Test
    public void testIsWorkingDay_AllDaysInWeek() {
        assertTrue(helper.isWorkingDay(LocalDate.of(2024, 11, 18)));  // Monday
        assertTrue(helper.isWorkingDay(LocalDate.of(2024, 11, 19)));  // Tuesday
        assertTrue(helper.isWorkingDay(LocalDate.of(2024, 11, 20)));  // Wednesday
        assertTrue(helper.isWorkingDay(LocalDate.of(2024, 11, 21)));  // Thursday
        assertTrue(helper.isWorkingDay(LocalDate.of(2024, 11, 22)));  // Friday
        assertFalse(helper.isWorkingDay(LocalDate.of(2024, 11, 23))); // Saturday
        assertFalse(helper.isWorkingDay(LocalDate.of(2024, 11, 24))); // Sunday
    }
}
