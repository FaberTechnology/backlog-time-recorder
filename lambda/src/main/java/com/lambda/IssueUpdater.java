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

    public Issue setActualHours(final int issueId) {
        final Issue issue = client.getIssue(issueId);
        final Optional<CustomField> startedAt = customField(issue, CUSTOM_FIELD_STARTED_AT);

        Duration elapsed = Duration.ZERO;
        if (startedAt.isPresent() && startedAt.get() instanceof TextCustomField) {
            final String startedAtValue = ((TextCustomField) startedAt.get()).getValue();

            if (startedAtValue != null && !startedAtValue.isBlank()) {
                elapsed = extracted(startedAtValue);
            }
        }

        if (elapsed == Duration.ZERO) {
            // get elapsed time from the creation of the issue in hours
            elapsed = new WorkdayUtils().calculateWorkingHours(
                    LocalDateTime.ofInstant(issue.getCreated().toInstant(), ZoneId.systemDefault()),
                    LocalDateTime.now());
        }
        DecimalFormat df = new DecimalFormat("0.0");
        float actualHours = Float.parseFloat((df.format(elapsed.toMinutes() / 60.0)));

        if (actualHours > 999 || actualHours < 0)
            return null;

        return client.updateIssue(new UpdateIssueParams(issue.getId()).actualHours(actualHours));
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

    public Issue setStartedAt(final int issueId) {
        final Issue issue = client.getIssue(issueId);
        final Optional<CustomField> field = customField(issue, CUSTOM_FIELD_STARTED_AT);

        if (!field.isPresent())
            return null;

        String startedAt = LocalDateTime.ofInstant(Instant.now(), JST_ZONE).toString();
        final String startedAtValue = ((TextCustomField) field.get()).getValue();
        if (startedAtValue != null && !startedAtValue.isBlank()) {
            startedAt = startedAtValue + ";" + startedAt;
        }
        return client.updateIssue(new UpdateIssueParams(issue.getId()).textCustomField(field.get().getId(), startedAt));
    }

    public Issue updateMilestones(final int issueId) {
        final Issue issue = client.getIssue(issueId);
        
        // Get start date and due date
        final Date startDate = issue.getStartDate();
        final Date dueDate = issue.getDueDate();
        
        if (startDate == null || dueDate == null) {
            return null;
        }
        
        // Convert to LocalDate (dates only, no time component)
        final LocalDate start = LocalDate.ofInstant(startDate.toInstant(), JST_ZONE);
        final LocalDate due = LocalDate.ofInstant(dueDate.toInstant(), JST_ZONE);
        
        // Calculate required milestones in YYYY-MMM format
        final Set<String> requiredMilestones = calculateRequiredMilestones(start, due);
        
        // Get current milestones
        final Set<String> currentMilestoneNames = issue.getMilestone().stream()
            .map(Milestone::getName)
            .collect(Collectors.toSet());
        
        // Get all project milestones
        final List<Milestone> projectMilestones = client.getMilestones(issue.getProjectId());
        
        // Find milestones to add (required but not already set)
        final Set<String> milestonesToAdd = requiredMilestones.stream()
            .filter(name -> !currentMilestoneNames.contains(name))
            .collect(Collectors.toSet());
        
        if (milestonesToAdd.isEmpty()) {
            return null;
        }
        
        // Get milestone IDs for the ones to add
        final List<Long> newMilestoneIds = projectMilestones.stream()
            .filter(m -> milestonesToAdd.contains(m.getName()))
            .map(Milestone::getId)
            .collect(Collectors.toList());
        
        // Combine existing milestone IDs with new ones
        final List<Long> allMilestoneIds = new ArrayList<>();
        allMilestoneIds.addAll(issue.getMilestone().stream()
            .map(Milestone::getId)
            .collect(Collectors.toList()));
        allMilestoneIds.addAll(newMilestoneIds);
        
        // Update issue with all milestones
        return client.updateIssue(new UpdateIssueParams(issue.getId())
            .milestoneIds(allMilestoneIds));
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
}
