package com.lambda.helpers;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.nulabinc.backlog4j.Milestone;

public class MilestoneHelper {

    public List<Long> calculateRequiredMilestones(
            final LocalDate start, final LocalDate due, final List<Milestone> projectMilestones) {
        final Set<String> requiredNames = calculateRequiredMilestoneNames(start, due);
        return projectMilestones.stream()
                .filter(m -> requiredNames.contains(m.getName()))
                .map(Milestone::getId)
                .collect(Collectors.toList());
    }

    Set<String> calculateRequiredMilestoneNames(final LocalDate start, final LocalDate due) {
        final Set<String> milestones = new HashSet<>();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MMM");

        YearMonth current = YearMonth.from(start);
        final YearMonth end = YearMonth.from(due);

        while (!current.isAfter(end)) {
            milestones.add(current.format(formatter));
            current = current.plusMonths(1);
        }

        return milestones;
    }
}
