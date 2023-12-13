package com.myorg;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionUrlAuthType;
import software.amazon.awscdk.services.lambda.FunctionUrlOptions;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.SnapStartConf;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

public class BacklogTimeRecorderStack extends Stack {
    public BacklogTimeRecorderStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public BacklogTimeRecorderStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        final Map<String, String> env = new HashMap<>() {
            {
                put("BACKLOG_API_KEY", System.getenv("BACKLOG_API_KEY"));
            }
        };

        final Function backlogTimeRecorder = Function.Builder.create(this, "BacklogTimeRecorder")
                .runtime(Runtime.JAVA_17)
                .code(Code.fromAsset("lambda/target/backlog-time-recorder-0.1.jar"))
                .handler("com.lambda.BacklogTimeRecorder::handleRequest")
                .environment(env)
                .timeout(Duration.seconds(30))
                .logRetention(RetentionDays.ONE_MONTH)
                .snapStart(SnapStartConf.ON_PUBLISHED_VERSIONS)
                .build();
        backlogTimeRecorder
                .addFunctionUrl(FunctionUrlOptions.builder()
                        .authType(FunctionUrlAuthType.NONE)
                        .build());
    }
}
