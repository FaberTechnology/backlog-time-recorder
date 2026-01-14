package com.lambda;

import com.lambda.utils.WorkdayUtils;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.nulabinc.backlog4j.BacklogClient;
import com.nulabinc.backlog4j.BacklogClientFactory;
import com.nulabinc.backlog4j.CustomField;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.Milestone;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;
import com.nulabinc.backlog4j.conf.BacklogConfigure;
import com.nulabinc.backlog4j.conf.BacklogJpConfigure;
import com.nulabinc.backlog4j.internal.json.customFields.TextCustomField;

public class IssueUpdater {
    private BacklogConfigure configure;
    private BacklogClient client;

    private static final String CUSTOM_FIELD_STARTED_AT = "Started at";
    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");

    public IssueUpdater(final String apiKey) {
        configure = new BacklogJpConfigure("faber-wi").apiKey(apiKey);
        client = new BacklogClientFactory(configure).newClient();
    }

    private Optional<CustomField> customField(final Issue issue, final String name) {
        return issue.getCustomFields().stream()
                .filter(fld -> fld.getName().equals(name))
                .findFirst();
    }

    /**
     * Calculate actual hours for an issue based on started at time or creation time.
     * @return actual hours, or null if invalid
     */
    private Float calculateActualHours(final Issue issue) {
        final Optional<CustomField> startedAt = customField(issue, CUSTOM_FIELD_STARTED_AT);
        Duration elapsed = Duration.ZERO;
        
        if (startedAt.isPresent() && startedAt.get() instanceof TextCustomField) {
            final String startedAtValue = ((TextCustomField) startedAt.get()).getValue();
            if (startedAtValue != null && !startedAtValue.isBlank()) {
                elapsed = extracted(startedAtValue);
            }
        }
        
        if (elapsed == Duration.ZERO) {
            elapsed = new WorkdayUtils().calculateWorkingHours(
                LocalDateTime.ofInstant(issue.getCreated().toInstant(), ZoneId.systemDefault()),
                LocalDateTime.now());
        }
        
        DecimalFormat df = new DecimalFormat("0.0");
        float actualHours = Float.parseFloat((df.format(elapsed.toMinutes() / 60.0)));
        
        if (actualHours < 0 || actualHours > 999) {
            return null;
        }
        
        return actualHours;
    }

    private Duration extracted(final String startedAtValue) {
        // get elapsed time from the started time in hours
        Duration elapsed = Duration.ZERO;
        try {
            final String[] startAtArray = startedAtValue.split(";");
            List<String> timesList = new ArrayList<>(Arrays.asList(startAtArray));
            timesList.add(LocalDateTime.ofInstant(Instant.now(), JST_ZONE).toString());

            for (int i = 0; i < timesList.size() - 1; i += 2) {
                LocalDateTime startAt = LocalDateTime.parse(timesList.get(i));
                LocalDateTime endAt = LocalDateTime.parse(timesList.get(i + 1));
                elapsed = elapsed.plus(new WorkdayUtils().calculateWorkingHours(startAt, endAt));
            }
        } catch (DateTimeParseException ex) {
        }
        return elapsed;
    }

    /**
     * Append current time to existing started at value.
     */
    private String appendCurrentTimeToStartedAt(final String existingValue) {
        String startedAt = LocalDateTime.ofInstant(Instant.now(), JST_ZONE).toString();
        if (existingValue != null && !existingValue.isBlank()) {
            startedAt = existingValue + ";" + startedAt;
        }
        return startedAt;
    }

    /**
     * Calculate milestone IDs to set based on issue dates.
     * @return list of milestone IDs including existing and new ones, or null if no changes needed
     */
    private List<Long> calculateMilestoneIdsToSet(final Issue issue) {
        final Date startDate = issue.getStartDate();
        final Date dueDate = issue.getDueDate();
        
        if (startDate == null || dueDate == null) {
            return null;
        }
        
        final LocalDate start = LocalDate.ofInstant(startDate.toInstant(), JST_ZONE);
        final LocalDate due = LocalDate.ofInstant(dueDate.toInstant(), JST_ZONE);
        final Set<String> requiredMilestones = calculateRequiredMilestones(start, due);
        
        final Set<String> currentMilestoneNames = issue.getMilestone().stream()
            .map(Milestone::getName)
            .collect(Collectors.toSet());
        
        final Set<String> milestonesToAdd = requiredMilestones.stream()
            .filter(name -> !currentMilestoneNames.contains(name))
            .collect(Collectors.toSet());
        
        if (milestonesToAdd.isEmpty()) {
            return null;
        }
        
        final List<Milestone> projectMilestones = client.getMilestones(issue.getProjectId());
        final List<Long> newMilestoneIds = projectMilestones.stream()
            .filter(m -> milestonesToAdd.contains(m.getName()))
            .map(Milestone::getId)
            .collect(Collectors.toList());
        
        final List<Long> allMilestoneIds = new ArrayList<>();
        allMilestoneIds.addAll(issue.getMilestone().stream()
            .map(Milestone::getId)
            .collect(Collectors.toList()));
        allMilestoneIds.addAll(newMilestoneIds);
        
        return allMilestoneIds;
    }
    
    private Set<String> calculateRequiredMilestones(final LocalDate start, final LocalDate due) {
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
    
    /**
     * Update issue with milestones, actual hours, and/or started time in a single API call.
     * This reduces the number of comments created on the issue.
     * 
     * @param issueId the issue ID
     * @param shouldUpdateMilestones whether to update milestones
     * @param shouldSetActualHours whether to set actual hours
     * @param shouldSetStartedAt whether to set started at time
     * @return the updated issue, or null if no updates were made
     */
    public Issue updateIssueFields(final int issueId, 
                                   final boolean shouldUpdateMilestones,
                                   final boolean shouldSetActualHours,
                                   final boolean shouldSetStartedAt) {
        final Issue issue = client.getIssue(issueId);
        final UpdateIssueParams params = new UpdateIssueParams(issue.getId());
        boolean hasUpdates = false;
        
        // Handle milestone updates
        if (shouldUpdateMilestones) {
            final List<Long> milestoneIds = calculateMilestoneIdsToSet(issue);
            if (milestoneIds != null) {
                params.milestoneIds(milestoneIds);
                hasUpdates = true;
            }
        }
        
        // Handle actual hours
        if (shouldSetActualHours) {
            final Float actualHours = calculateActualHours(issue);
            if (actualHours != null) {
                params.actualHours(actualHours);
                hasUpdates = true;
            }
        }
        
        // Handle started at time
        if (shouldSetStartedAt) {
            final Optional<CustomField> field = customField(issue, CUSTOM_FIELD_STARTED_AT);
            if (field.isPresent()) {
                final String startedAt = appendCurrentTimeToStartedAt(
                    ((TextCustomField) field.get()).getValue());
                params.textCustomField(field.get().getId(), startedAt);
                hasUpdates = true;
            }
        }
        
        // Only update if there are changes
        if (!hasUpdates) {
            return null;
        }
        
        return client.updateIssue(params);
    }
}
