package com.lambda.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.nulabinc.backlog4j.Issue.StatusType;

public class RestrictedStatusTransitionPolicy {

    private static final String PBI_ISSUE_TYPE_NAME = "PBI";

    private final Set<Long> productOwnerUserIds;
    private final Set<Integer> settingPriorityStatusIds;

    public RestrictedStatusTransitionPolicy(final Set<Long> productOwnerUserIds,
            final Set<Integer> settingPriorityStatusIds) {
        this.productOwnerUserIds = productOwnerUserIds;
        this.settingPriorityStatusIds = settingPriorityStatusIds;
    }

    public static RestrictedStatusTransitionPolicy fromEnv() {
        return new RestrictedStatusTransitionPolicy(
                parseUserIds(System.getenv("PRODUCT_OWNER_USER_IDS")),
                parseStatusIds(System.getenv("SETTING_PRIORITY_STATUS_IDS")));
    }

    static Set<Long> parseUserIds(final String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }

    static Set<Integer> parseStatusIds(final String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    public boolean isRestrictedTransition(final int oldStatusCode, final int newStatusCode) {
        if (productOwnerUserIds.isEmpty()) {
            return false;
        }
        if (newStatusCode == StatusType.Closed.getIntValue()) {
            return true;
        }
        return oldStatusCode == StatusType.Open.getIntValue() && settingPriorityStatusIds.contains(newStatusCode);
    }

    public boolean isAuthorized(final long actorUserId) {
        return productOwnerUserIds.contains(actorUserId);
    }

    public boolean isPbiIssueType(final String issueTypeName) {
        return PBI_ISSUE_TYPE_NAME.equals(issueTypeName);
    }
}
