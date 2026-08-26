package com.lambda.models;

import java.util.Set;

import com.nulabinc.backlog4j.Issue.StatusType;

public class RestrictedStatusTransitionPolicy {

    private final Set<Long> productOwnerUserIds;
    private final int settingPriorityStatusId;

    public RestrictedStatusTransitionPolicy(final Set<Long> productOwnerUserIds, final int settingPriorityStatusId) {
        this.productOwnerUserIds = productOwnerUserIds;
        this.settingPriorityStatusId = settingPriorityStatusId;
    }

    public boolean isRestrictedTransition(final int oldStatusCode, final int newStatusCode) {
        if (productOwnerUserIds.isEmpty()) {
            return false;
        }
        if (newStatusCode == StatusType.Closed.getIntValue()) {
            return true;
        }
        return oldStatusCode == StatusType.Open.getIntValue() && newStatusCode == settingPriorityStatusId;
    }

    public boolean isAuthorized(final long actorUserId) {
        return productOwnerUserIds.contains(actorUserId);
    }
}
