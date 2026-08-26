package com.lambda.handlers;

public interface StatusChangeNotifier {
    void notifyUnauthorizedStatusChange(int issueId, int oldStatusCode, int newStatusCode, long actorUserId);
}
