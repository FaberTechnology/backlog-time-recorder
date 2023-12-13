package com.lambda.models;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nulabinc.backlog4j.Change;
import com.nulabinc.backlog4j.Status;
import com.nulabinc.backlog4j.internal.json.ChangeJSONImpl;
import com.nulabinc.backlog4j.internal.json.StatusJSONImpl;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Issue {
    int id;
    @JsonDeserialize(as = ChangeJSONImpl[].class)
    Change[] changes;
    BigDecimal actualHours;
    @JsonDeserialize(as = StatusJSONImpl.class)
    Status status;
    String summary;

    public int getId() {
        return id;
    }

    public List<Change> getChanges() {
        if (changes == null || changes.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(changes);
    }

    public BigDecimal getActualHours() {
        return actualHours;
    }

    public Status getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }
}
