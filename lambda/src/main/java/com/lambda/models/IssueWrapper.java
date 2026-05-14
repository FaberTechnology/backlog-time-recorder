package com.lambda.models;

import com.nulabinc.backlog4j.CustomField;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.Milestone;
import com.nulabinc.backlog4j.internal.json.customFields.TextCustomField;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class IssueWrapper {

    private static final String CUSTOM_FIELD_STARTED_AT = "Started at";

    private final Issue rawIssue;

    public IssueWrapper(Issue rawIssue) {
        this.rawIssue = rawIssue;
    }

    public long getId() {
        return rawIssue.getId();
    }

    public long getProjectId() {
        return rawIssue.getProjectId();
    }

    public List<Milestone> getIssueMilestones() {
        return rawIssue.getMilestone();
    }

    public Date getStartDate() {
        return rawIssue.getStartDate();
    }

    public Date getDueDate() {
        return rawIssue.getDueDate();
    }

    public BigDecimal getActualHours() {
        return rawIssue.getActualHours();
    }

    public Date getCreated() {
        return rawIssue.getCreated();
    }

    public Issue.StatusType getStatus() {
        return rawIssue.getStatus().getStatusType();
    }

    public Optional<CustomField> getCustomField(String name) {
        return rawIssue.getCustomFields().stream()
                .filter(fld -> fld.getName().equals(name))
                .findFirst();
    }

    public Optional<String> getStartedAtValue() {
        return getCustomField(CUSTOM_FIELD_STARTED_AT)
                .filter(f -> f instanceof TextCustomField)
                .map(f -> ((TextCustomField) f).getValue())
                .filter(v -> v != null && !v.isBlank());
    }

    public Optional<Long> getStartedAtFieldId() {
        return getCustomField(CUSTOM_FIELD_STARTED_AT)
                .map(CustomField::getId);
    }

    public Issue unwrap() {
        return rawIssue;
    }
}
