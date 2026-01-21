package com.lambda;

import com.lambda.utils.IssueUtils;
import com.lambda.utils.WorkdayUtils;
import com.nulabinc.backlog4j.CustomField;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.internal.json.customFields.TextCustomField;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TimeCalculator {

    public static class TimeUpdateData {
        private Float actualHours;
        private String startedAt;

        public boolean hasTimeUpdates() {
            return actualHours != null || startedAt != null;
        }

        public Float getActualHours() {
            return actualHours;
        }

        public void setActualHours(Float actualHours) {
            this.actualHours = actualHours;
        }

        public String getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(String startedAt) {
            this.startedAt = startedAt;
        }
    }

    /**
     * Calculate time-related updates for an issue.
     */
    public TimeUpdateData calculateTimeUpdates(Issue issue, EnumSet<IssueUpdater.UpdateField> fieldsToUpdate) {
        TimeUpdateData updateData = new TimeUpdateData();

        if (fieldsToUpdate.contains(IssueUpdater.UpdateField.ACTUAL_HOURS)) {
            updateData.setActualHours(calculateActualHours(issue));
        }

        if (fieldsToUpdate.contains(IssueUpdater.UpdateField.STARTED_AT)) {
            final Optional<CustomField> field = IssueUtils.customField(issue, AppConstants.CUSTOM_FIELD_STARTED_AT);
            if (field.isPresent()) {
                updateData.setStartedAt(appendCurrentTimeToStartedAt(
                    ((TextCustomField) field.get()).getValue()));
            }
        }

        return updateData;
    }

    /**
     * Calculate actual hours for an issue based on started at time or creation time.
     */
    public Float calculateActualHours(final Issue issue) {
        final Optional<CustomField> startedAt = IssueUtils.customField(issue, AppConstants.CUSTOM_FIELD_STARTED_AT);
        Duration elapsed = Duration.ZERO;
        
        if (startedAt.isPresent() && startedAt.get() instanceof TextCustomField) {
            final String startedAtValue = ((TextCustomField) startedAt.get()).getValue();
            if (startedAtValue != null && !startedAtValue.isBlank()) {
                elapsed = extractElapsedTime(startedAtValue);
            }
        }
        
        if (elapsed == Duration.ZERO) {
            elapsed = WorkdayUtils.calculateWorkingHours(
                LocalDateTime.ofInstant(issue.getCreated().toInstant(), AppConstants.SYSTEM_ZONE),
                LocalDateTime.now());
        }
        
        DecimalFormat df = new DecimalFormat(AppConstants.DECIMAL_FORMAT_HOURS);
        float actualHours = Float.parseFloat((df.format(elapsed.toMinutes() / 60.0)));
        
        if (actualHours < 0 || actualHours > AppConstants.MAX_ACTUAL_HOURS) {
            return null;
        }
        
        return actualHours;
    }

    /**
     * Extract elapsed time from the started at field value.
     */
    private Duration extractElapsedTime(final String startedAtValue) {
        Duration elapsed = Duration.ZERO;
        try {
            final String[] startAtArray = startedAtValue.split(AppConstants.TIME_SEPARATOR);
            List<String> timesList = new ArrayList<>(Arrays.asList(startAtArray));
            timesList.add(LocalDateTime.ofInstant(Instant.now(), AppConstants.JST_ZONE).toString());

            for (int i = 0; i < timesList.size() - 1; i += 2) {
                LocalDateTime startAt = LocalDateTime.parse(timesList.get(i));
                LocalDateTime endAt = LocalDateTime.parse(timesList.get(i + 1));
                elapsed = elapsed.plus(WorkdayUtils.calculateWorkingHours(startAt, endAt));
            }
        } catch (DateTimeParseException ex) {
            System.err.println("Failed to parse started at timestamps: " + ex.getMessage());
        }
        return elapsed;
    }

    /**
     * Append current time to existing started at value.
     */
    private String appendCurrentTimeToStartedAt(final String existingValue) {
        String startedAt = LocalDateTime.ofInstant(Instant.now(), AppConstants.JST_ZONE).toString();
        if (existingValue != null && !existingValue.isBlank()) {
            startedAt = existingValue + AppConstants.TIME_SEPARATOR + startedAt;
        }
        return startedAt;
    }

    /**
     * Calculate required milestone names for a date range.
     */
    public Set<String> calculateRequiredMilestones(final LocalDate start, final LocalDate due) {
        final Set<String> milestones = new HashSet<>();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT_MILESTONE);
        
        YearMonth current = YearMonth.from(start);
        final YearMonth end = YearMonth.from(due);
        
        while (!current.isAfter(end)) {
            milestones.add(current.format(formatter));
            current = current.plusMonths(1);
        }
        
        return milestones;
    }
}