package com.lambda.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.nulabinc.backlog4j.CustomField;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.Milestone;

public class IssueWrapper {

    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");

    private final Issue rawIssue;
    private final int newStatusCode;

    public IssueWrapper(final Issue rawIssue, final int newStatusCode) {
        this.rawIssue = rawIssue;
        this.newStatusCode = newStatusCode;
    }

    public int getId() {
        return (int) rawIssue.getId();
    }

    public long getProjectId() {
        return rawIssue.getProjectId();
    }

    public List<Long> getIssueKeyMilestones() {
        return rawIssue.getMilestone().stream()
                .map(Milestone::getId)
                .collect(Collectors.toList());
    }

    public Optional<LocalDate> getStartDate() {
        final Date date = rawIssue.getStartDate();
        if (date == null) return Optional.empty();
        return Optional.of(LocalDate.ofInstant(date.toInstant(), JST_ZONE));
    }

    public Optional<LocalDate> getDueDate() {
        final Date date = rawIssue.getDueDate();
        if (date == null) return Optional.empty();
        return Optional.of(LocalDate.ofInstant(date.toInstant(), JST_ZONE));
    }

    public Optional<Float> getActualHours() {
        return Optional.ofNullable(rawIssue.getActualHours());
    }

    public LocalDateTime getCreatedAt() {
        return LocalDateTime.ofInstant(rawIssue.getCreated().toInstant(), ZoneId.systemDefault());
    }

    public int getNewStatusCode() {
        return newStatusCode;
    }

    public Optional<CustomField> getCustomField(final String name) {
        return rawIssue.getCustomFields().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst();
    }
}
