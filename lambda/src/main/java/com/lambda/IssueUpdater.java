package com.lambda;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.nulabinc.backlog4j.BacklogClient;
import com.nulabinc.backlog4j.BacklogClientFactory;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;
import com.nulabinc.backlog4j.conf.BacklogConfigure;
import com.nulabinc.backlog4j.conf.BacklogJpConfigure;

public class IssueUpdater {
    private BacklogConfigure configure;
    private BacklogClient client;

    public IssueUpdater(final String apiKey) {
        configure = new BacklogJpConfigure("faber-wi").apiKey(apiKey);
        client = new BacklogClientFactory(configure).newClient();
    }

    public Issue setRealHours(final int issueId) {
        final Issue issue = client.getIssue(issueId);

        // get elapsed time from the creation of the issue in hours
        final Duration elapsed = Duration.between(
                LocalDateTime.ofInstant(issue.getCreated().toInstant(), ZoneId.systemDefault()),
                LocalDateTime.now());

        return client.updateIssue(new UpdateIssueParams(issueId).actualHours(elapsed.toHours()));
    }
}
