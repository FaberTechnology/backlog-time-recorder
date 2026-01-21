package com.lambda;

import com.lambda.TimeCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IssueUpdaterTest {
    
    private final TimeCalculator calculator = new TimeCalculator();

    @Test
    public void testCalculateRequiredMilestones_SameMonth() {
        // Test when start and due date are in the same month
        LocalDate start = LocalDate.of(2026, Month.JANUARY, 5);
        LocalDate due = LocalDate.of(2026, Month.JANUARY, 25);

        Set<String> milestones = calculator.calculateRequiredMilestones(start, due);

        assertEquals(1, milestones.size());
        assertTrue(milestones.contains("2026-Jan"));
    }

    @Test
    public void testCalculateRequiredMilestones_TwoMonths() {
        // Test when start and due date span two months
        LocalDate start = LocalDate.of(2026, Month.JANUARY, 15);
        LocalDate due = LocalDate.of(2026, Month.FEBRUARY, 10);

        Set<String> milestones = calculator.calculateRequiredMilestones(start, due);

        assertEquals(2, milestones.size());
        assertTrue(milestones.contains("2026-Jan"));
        assertTrue(milestones.contains("2026-Feb"));
    }

    @Test
    public void testCalculateRequiredMilestones_MultipleMonths() {
        // Test when start and due date span multiple months
        LocalDate start = LocalDate.of(2026, Month.JANUARY, 15);
        LocalDate due = LocalDate.of(2026, Month.APRIL, 30);

        Set<String> milestones = calculator.calculateRequiredMilestones(start, due);

        assertEquals(4, milestones.size());
        assertTrue(milestones.contains("2026-Jan"));
        assertTrue(milestones.contains("2026-Feb"));
        assertTrue(milestones.contains("2026-Mar"));
        assertTrue(milestones.contains("2026-Apr"));
    }

    @Test
    public void testCalculateRequiredMilestones_AcrossYears() {
        // Test when start and due date span across year boundary
        LocalDate start = LocalDate.of(2025, Month.DECEMBER, 15);
        LocalDate due = LocalDate.of(2026, Month.FEBRUARY, 28);

        Set<String> milestones = calculator.calculateRequiredMilestones(start, due);

        assertEquals(3, milestones.size());
        assertTrue(milestones.contains("2025-Dec"));
        assertTrue(milestones.contains("2026-Jan"));
        assertTrue(milestones.contains("2026-Feb"));
    }

    @Test
    public void testCalculateRequiredMilestones_FullYear() {
        // Test when start and due date span a full year
        LocalDate start = LocalDate.of(2026, Month.JANUARY, 1);
        LocalDate due = LocalDate.of(2026, Month.DECEMBER, 31);

        Set<String> milestones = calculator.calculateRequiredMilestones(start, due);

        assertEquals(12, milestones.size());
        assertTrue(milestones.contains("2026-Jan"));
        assertTrue(milestones.contains("2026-Feb"));
        assertTrue(milestones.contains("2026-Mar"));
        assertTrue(milestones.contains("2026-Apr"));
        assertTrue(milestones.contains("2026-May"));
        assertTrue(milestones.contains("2026-Jun"));
        assertTrue(milestones.contains("2026-Jul"));
        assertTrue(milestones.contains("2026-Aug"));
        assertTrue(milestones.contains("2026-Sep"));
        assertTrue(milestones.contains("2026-Oct"));
        assertTrue(milestones.contains("2026-Nov"));
        assertTrue(milestones.contains("2026-Dec"));
    }

    @Test
    public void testCalculateRequiredMilestones_FirstAndLastDayOfMonth() {
        // Test edge case: first day to last day of different months
        LocalDate start = LocalDate.of(2026, Month.MARCH, 1);
        LocalDate due = LocalDate.of(2026, Month.MAY, 31);

        Set<String> milestones = calculator.calculateRequiredMilestones(start, due);

        assertEquals(3, milestones.size());
        assertTrue(milestones.contains("2026-Mar"));
        assertTrue(milestones.contains("2026-Apr"));
        assertTrue(milestones.contains("2026-May"));
    }

    @Test
    public void testCalculateRequiredMilestones_LeapYear() {
        // Test leap year scenario
        LocalDate start = LocalDate.of(2024, Month.FEBRUARY, 1);
        LocalDate due = LocalDate.of(2024, Month.FEBRUARY, 29);

        Set<String> milestones = calculator.calculateRequiredMilestones(start, due);

        assertEquals(1, milestones.size());
        assertTrue(milestones.contains("2024-Feb"));
    }


}
