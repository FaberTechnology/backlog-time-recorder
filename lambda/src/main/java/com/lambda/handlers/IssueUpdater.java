package com.lambda.handlers;

public interface IssueUpdater {
    com.nulabinc.backlog4j.Issue updateIssue(int issueId, int newStatusCode);
}
