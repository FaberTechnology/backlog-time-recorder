package com.lambda.helpers;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.nulabinc.backlog4j.Milestone;

public class MilestoneHelper {

    private static final DateTimeFormatter MONTHLY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MMM", Locale.ENGLISH);

    private static final Pattern MONTHLY_NAME_PATTERN = Pattern.compile(
            "^\\d{4}-(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)$");

    public List<Long> calculateRequiredMilestones(
            final LocalDate start, final LocalDate due, final List<Milestone> projectMilestones) {
        final Set<String> requiredNames = calculateRequiredMilestoneNames(start, due);
        return projectMilestones.stream()
                .filter(m -> requiredNames.contains(m.getName()))
                .map(Milestone::getId)
                .collect(Collectors.toList());
    }

    public Set<String> calculateRequiredMilestoneNames(final LocalDate start, final LocalDate due) {
        final Set<String> milestones = new LinkedHashSet<>();
        YearMonth current = YearMonth.from(start);
        final YearMonth end = YearMonth.from(due);
        while (!current.isAfter(end)) {
            milestones.add(current.format(MONTHLY_FORMATTER));
            current = current.plusMonths(1);
        }
        return milestones;
    }

    public boolean isMonthlyMilestoneName(final String name) {
        return name != null && MONTHLY_NAME_PATTERN.matcher(name).matches();
    }

    public LocalDate monthStartDate(final String monthlyName) {
        return parseYearMonth(monthlyName).atDay(1);
    }

    public LocalDate monthEndDate(final String monthlyName) {
        return parseYearMonth(monthlyName).atEndOfMonth();
    }

    private YearMonth parseYearMonth(final String monthlyName) {
        if (!isMonthlyMilestoneName(monthlyName)) {
            throw new IllegalArgumentException("Not a monthly milestone name: " + monthlyName);
        }
        try {
            return YearMonth.parse(monthlyName, MONTHLY_FORMATTER);
        } catch (final DateTimeParseException e) {
            throw new IllegalArgumentException("Not a monthly milestone name: " + monthlyName, e);
        }
    }
}
