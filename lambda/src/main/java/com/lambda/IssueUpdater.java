package com.lambda;

import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;

import com.nulabinc.backlog4j.BacklogClient;
import com.nulabinc.backlog4j.BacklogClientFactory;
import com.nulabinc.backlog4j.CustomField;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;
import com.nulabinc.backlog4j.conf.BacklogConfigure;
import com.nulabinc.backlog4j.conf.BacklogJpConfigure;
import com.nulabinc.backlog4j.internal.json.customFields.TextCustomField;

public class IssueUpdater {
    private BacklogConfigure configure;
    private BacklogClient client;

    private static final String CUSTOM_FIELD_STARTED_AT = "Started at";
    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");
    private static final List<DayOfWeek> WEEKENDS = Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 30); // Start at 09:30 JST
    private static final LocalTime WORK_END_TIME = LocalTime.of(19, 30); // End at 17:30 ICT as 19:30 JST


    public IssueUpdater(final String apiKey) {
        configure = new BacklogJpConfigure("faber-wi").apiKey(apiKey);
        client = new BacklogClientFactory(configure).newClient();
    }

    private Optional<CustomField> customField(final Issue issue, final String name) {
        return issue.getCustomFields().stream()
                .filter(fld -> fld.getName().equals(name))
                .findFirst();
    }

    public Issue setActualHours(final int issueId) {
        final Issue issue = client.getIssue(issueId);
        final Optional<CustomField> startedAt = customField(issue, CUSTOM_FIELD_STARTED_AT);

        Duration elapsed = Duration.ZERO;
        if (startedAt.isPresent() && startedAt.get() instanceof TextCustomField) {
            final String startedAtValue = ((TextCustomField) startedAt.get()).getValue();

            if (startedAtValue != null && !startedAtValue.isBlank()) {
                // get elapsed time from the started time in hours
                try {
                    LocalDateTime endAt = LocalDateTime.ofInstant(Instant.now(), JST_ZONE);
                    elapsed = calculateWorkingHours(LocalDateTime.parse(startedAtValue), endAt);
                } catch (DateTimeParseException ex) {
                }
            }
        }

        if (elapsed == Duration.ZERO) {
            // get elapsed time from the creation of the issue in hours
            elapsed = Duration.between(
                    LocalDateTime.ofInstant(issue.getCreated().toInstant(), ZoneId.systemDefault()),
                    LocalDateTime.now());
        }
        DecimalFormat df = new DecimalFormat("0.0");
        float actualHours = Float.parseFloat((df.format(elapsed.toMinutes() / 60.0)));

        if (actualHours > 999 || actualHours < 0)
            return null;

        return client.updateIssue(new UpdateIssueParams(issue.getId()).actualHours(actualHours));
    }

    private Duration calculateWorkingHours(final LocalDateTime start, final LocalDateTime end) {
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

    public Issue setStartedAt(final int issueId) {
        final Issue issue = client.getIssue(issueId);
        final Optional<CustomField> field = customField(issue, CUSTOM_FIELD_STARTED_AT);

        if (!field.isPresent())
            return null;

        String startedAt = LocalDateTime.ofInstant(Instant.now(), JST_ZONE).toString();
        return client.updateIssue(new UpdateIssueParams(issue.getId()).textCustomField(field.get().getId(), startedAt));
    }
}
