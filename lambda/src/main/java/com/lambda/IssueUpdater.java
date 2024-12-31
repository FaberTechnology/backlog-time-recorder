package com.lambda;

import com.lambda.utils.WorkdayUtils;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Optional;

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
                    elapsed = new WorkdayUtils().calculateWorkingHours(LocalDateTime.parse(startedAtValue), endAt);
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

    public Issue setStartedAt(final int issueId) {
        final Issue issue = client.getIssue(issueId);
        final Optional<CustomField> field = customField(issue, CUSTOM_FIELD_STARTED_AT);

        if (!field.isPresent())
            return null;

        String startedAt = LocalDateTime.ofInstant(Instant.now(), JST_ZONE).toString();
        final String startedAtValue = ((TextCustomField) startedAt.get()).getValue();
        if (startedAtValue != null && !startedAtValue.isBlank()) {
            startedAt = startedAtValue + ";" + startedAt;
        }
        return client.updateIssue(new UpdateIssueParams(issue.getId()).textCustomField(field.get().getId(), startedAt));
    }
}
