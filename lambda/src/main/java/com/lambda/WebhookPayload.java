package com.lambda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lambda.models.Issue;
import com.nulabinc.backlog4j.Project;
import com.nulabinc.backlog4j.User;
import com.nulabinc.backlog4j.internal.json.ProjectJSONImpl;
import com.nulabinc.backlog4j.internal.json.UserJSONImpl;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayload {
    int id;
    @JsonDeserialize(as = ProjectJSONImpl.class)
    Project project;
    int type;
    @JsonDeserialize(as = Issue.class)
    Issue content;
    @JsonDeserialize(as = UserJSONImpl.class)
    User createdUser;
    String created;
}
