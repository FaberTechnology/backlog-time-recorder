package com.lambda.models;

import com.nulabinc.backlog4j.internal.json.Jackson;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class IssueDeserializationTest {

    @Test
    public void testDeserializeIssueWithDates() {
        String json = """
            {
                "id": 123,
                "summary": "Test Issue",
                "startDate": "2023-11-16",
                "dueDate": "2023-11-20"
            }
            """;

        Issue issue = Jackson.fromJsonString(json, Issue.class);

        assertNotNull(issue);
        assertEquals(123, issue.getId());
        assertEquals("Test Issue", issue.getSummary());
        
        // Verify dates are parsed correctly
        assertNotNull(issue.getStartDate());
        assertNotNull(issue.getDueDate());
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals("2023-11-16", sdf.format(issue.getStartDate()));
        assertEquals("2023-11-20", sdf.format(issue.getDueDate()));
    }

    @Test
    public void testDeserializeIssueWithNullDates() {
        String json = """
            {
                "id": 456,
                "summary": "Test Issue Without Dates",
                "startDate": null,
                "dueDate": null
            }
            """;

        Issue issue = Jackson.fromJsonString(json, Issue.class);

        assertNotNull(issue);
        assertEquals(456, issue.getId());
        assertEquals("Test Issue Without Dates", issue.getSummary());
        
        // Verify dates are null
        assertNull(issue.getStartDate());
        assertNull(issue.getDueDate());
    }

    @Test
    public void testDeserializeIssueWithoutDateFields() {
        String json = """
            {
                "id": 789,
                "summary": "Test Issue Missing Date Fields"
            }
            """;

        Issue issue = Jackson.fromJsonString(json, Issue.class);

        assertNotNull(issue);
        assertEquals(789, issue.getId());
        assertEquals("Test Issue Missing Date Fields", issue.getSummary());
        
        // Verify dates are null when not present in JSON
        assertNull(issue.getStartDate());
        assertNull(issue.getDueDate());
    }

    @Test
    public void testDeserializeRealWebhookPayload() {
        // Real payload from issue.json
        String json = """
            {
                "id": 39751072,
                "key_id": 809,
                "summary": "Test summary",
                "startDate": "2023-11-16",
                "dueDate": "2023-11-20",
                "estimatedHours": null,
                "actualHours": null
            }
            """;

        Issue issue = Jackson.fromJsonString(json, Issue.class);

        assertNotNull(issue);
        assertEquals(39751072, issue.getId());
        
        assertNotNull(issue.getStartDate());
        assertNotNull(issue.getDueDate());
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals("2023-11-16", sdf.format(issue.getStartDate()));
        assertEquals("2023-11-20", sdf.format(issue.getDueDate()));
    }
}
