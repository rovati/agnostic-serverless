package ch.elca.rovl.dsl.pipeline.deployment.accessinfo.queue;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

/**
 * Object containing access information for a queue on AWS.
 */
public class AwsQueueAccess extends QueueAccess {
    String queueUrl;
    String queueArn;
    String sendPolicyArn;
    String listenPolicyArn;
    final AwsSessionCredentials credentials;

    public AwsQueueAccess(AwsSessionCredentials credentials) {
        this.credentials = credentials;
    }

    public void setSendPolicyArn(String policyArn) {
        this.sendPolicyArn = policyArn;
    }

    public void setReceivePolicyArn(String policyArn) {
        this.listenPolicyArn = policyArn;
    }

    public void setQueueUrl(String url) {
        this.queueUrl = url;
    }

    public void setQueueArn(String arn) {
        this.queueArn = arn;
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public String getQueueArn() {
        return queueArn;
    }

    public String getSendPolicyArn() {
        return sendPolicyArn;
    }

    public String getReceivePolicyArn() {
        return listenPolicyArn;
    }

    public AwsSessionCredentials getCredentials() {
        return credentials;
    }
}
