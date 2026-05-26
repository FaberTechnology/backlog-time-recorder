package com.lambda.helpers;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MilestoneHelperTest {

    private final MilestoneHelper helper = new MilestoneHelper();

    @Test
    public void testCalculateRequiredMilestones_SameMonth() {
        LocalDate start = LocalDate.of(2026, Month.JANUARY, 5);
        LocalDate due = LocalDate.of(2026, Month.JANUARY, 25);

        Set<String> milestones = helper.calculateRequiredMilestoneNames(start, due);

        assertEquals(1, milestones.size());
        assertTrue(milestones.contains("2026-Jan"));
    }

    @Test
    public void testCalculateRequiredMilestones_TwoMonths() {
        LocalDate start = LocalDate.of(2026, Month.JANUARY, 15);
        LocalDate due = LocalDate.of(2026, Month.FEBRUARY, 10);

        Set<String> milestones = helper.calculateRequiredMilestoneNames(start, due);

        assertEquals(2, milestones.size());
        assertTrue(milestones.contains("2026-Jan"));
        assertTrue(milestones.contains("2026-Feb"));
    }

    @Test
    public void testCalculateRequiredMilestones_MultipleMonths() {
        LocalDate start = LocalDate.of(2026, Month.JANUARY, 15);
        LocalDate due = LocalDate.of(2026, Month.APRIL, 30);

        Set<String> milestones = helper.calculateRequiredMilestoneNames(start, due);

        assertEquals(4, milestones.size());
        assertTrue(milestones.contains("2026-Jan"));
        assertTrue(milestones.contains("2026-Feb"));
        assertTrue(milestones.contains("2026-Mar"));
        assertTrue(milestones.contains("2026-Apr"));
    }

    @Test
    public void testCalculateRequiredMilestones_AcrossYears() {
        LocalDate start = LocalDate.of(2025, Month.DECEMBER, 15);
        LocalDate due = LocalDate.of(2026, Month.FEBRUARY, 28);

        Set<String> milestones = helper.calculateRequiredMilestoneNames(start, due);

        assertEquals(3, milestones.size());
        assertTrue(milestones.contains("2025-Dec"));
        assertTrue(milestones.contains("2026-Jan"));
        assertTrue(milestones.contains("2026-Feb"));
    }

    @Test
    public void testCalculateRequiredMilestones_FullYear() {
        LocalDate start = LocalDate.of(2026, Month.JANUARY, 1);
        LocalDate due = LocalDate.of(2026, Month.DECEMBER, 31);

        Set<String> milestones = helper.calculateRequiredMilestoneNames(start, due);

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
        LocalDate start = LocalDate.of(2026, Month.MARCH, 1);
        LocalDate due = LocalDate.of(2026, Month.MAY, 31);

        Set<String> milestones = helper.calculateRequiredMilestoneNames(start, due);

        assertEquals(3, milestones.size());
        assertTrue(milestones.contains("2026-Mar"));
        assertTrue(milestones.contains("2026-Apr"));
        assertTrue(milestones.contains("2026-May"));
    }

    @Test
    public void testCalculateRequiredMilestones_LeapYear() {
        LocalDate start = LocalDate.of(2024, Month.FEBRUARY, 1);
        LocalDate due = LocalDate.of(2024, Month.FEBRUARY, 29);

        Set<String> milestones = helper.calculateRequiredMilestoneNames(start, due);

        assertEquals(1, milestones.size());
        assertTrue(milestones.contains("2024-Feb"));
    }

    @Test
    public void testIsMonthlyMilestoneName_ValidNames() {
        assertTrue(helper.isMonthlyMilestoneName("2026-Jan"));
        assertTrue(helper.isMonthlyMilestoneName("2026-May"));
        assertTrue(helper.isMonthlyMilestoneName("2099-Dec"));
    }

    @Test
    public void testIsMonthlyMilestoneName_InvalidNames() {
        assertFalse(helper.isMonthlyMilestoneName(null));
        assertFalse(helper.isMonthlyMilestoneName(""));
        assertFalse(helper.isMonthlyMilestoneName("Sprint 1"));
        assertFalse(helper.isMonthlyMilestoneName("2026-may"));
        assertFalse(helper.isMonthlyMilestoneName("2026-MAY"));
        assertFalse(helper.isMonthlyMilestoneName("26-May"));
        assertFalse(helper.isMonthlyMilestoneName("2026-Mayy"));
        assertFalse(helper.isMonthlyMilestoneName("v1.0"));
    }

    @Test
    public void testMonthStartDate() {
        assertEquals(LocalDate.of(2026, Month.MAY, 1), helper.monthStartDate("2026-May"));
        assertEquals(LocalDate.of(2024, Month.FEBRUARY, 1), helper.monthStartDate("2024-Feb"));
    }

    @Test
    public void testMonthEndDate() {
        assertEquals(LocalDate.of(2026, Month.MAY, 31), helper.monthEndDate("2026-May"));
        assertEquals(LocalDate.of(2024, Month.FEBRUARY, 29), helper.monthEndDate("2024-Feb"));
        assertEquals(LocalDate.of(2025, Month.FEBRUARY, 28), helper.monthEndDate("2025-Feb"));
    }

    @Test
    public void testMonthDate_RejectsNonMonthlyName() {
        assertThrows(IllegalArgumentException.class, () -> helper.monthStartDate("Sprint 1"));
        assertThrows(IllegalArgumentException.class, () -> helper.monthEndDate("2026-may"));
    }
}
