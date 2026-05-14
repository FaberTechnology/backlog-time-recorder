package com.lambda.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import java.math.BigDecimal;

import com.nulabinc.backlog4j.CustomField;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.Milestone;

public class IssueWrapper {

    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");

    private final Issue rawIssue;

    public IssueWrapper(final Issue rawIssue) {
        this.rawIssue = rawIssue;
    }

    public long getId() {
        return rawIssue.getId();
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
        final BigDecimal value = rawIssue.getActualHours();
        return value == null ? Optional.empty() : Optional.of(value.floatValue());
    }

    public LocalDateTime getCreatedAt() {
        return LocalDateTime.ofInstant(rawIssue.getCreated().toInstant(), ZoneId.systemDefault());
    }

    public Optional<CustomField> getCustomField(final String name) {
        return rawIssue.getCustomFields().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst();
    }
}
