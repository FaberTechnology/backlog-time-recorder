package com.lambda.utils;

import com.nulabinc.backlog4j.CustomField;
import com.nulabinc.backlog4j.Issue;

import java.util.Optional;

/**
 * Utility class for common operations across the application.
 */
public final class IssueUtils {

    private IssueUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Find a custom field by name in an issue.
     */
    public static Optional<CustomField> customField(final Issue issue, final String name) {
        return issue.getCustomFields().stream()
                .filter(fld -> fld.getName().equals(name))
                .findFirst();
    }
}