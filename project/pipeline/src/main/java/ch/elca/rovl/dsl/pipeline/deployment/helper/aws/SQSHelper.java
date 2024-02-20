package ch.elca.rovl.dsl.pipeline.deployment.helper.aws;

import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

public class SQSHelper {
    
    SqsClient client;

    public SQSHelper() {
        this.client = SqsClient.builder()
            .httpClientBuilder(ApacheHttpClient.builder())
            .build();
    }

    /**
     * Returns the url of the sqs queue with the given name, and creates the queue if it does not exist.
     * @param queueName name of the sqs queue
     * @return the url of the sqs queue
     */
    public String getOrCreateQueue(String queueName) {
        try {
            return client.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build()).queueUrl();
        } catch(QueueDoesNotExistException e) {
            return client.createQueue(CreateQueueRequest.builder()
                .queueName(queueName)
                .build())
            .queueUrl();
        }
    }

    /**
     * Returns the arn of the sqs queue with the given url, or null if the queue does not exist.
     * @param queueUrl url of the sqs queue
     * @return the arn of the sqs queue, or null if the queue does not exist
     */
    public String getQueueArn(String queueUrl) {
        try {
            return client.getQueueAttributes(GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.QUEUE_ARN)
                    .build()).attributes().get(QueueAttributeName.QUEUE_ARN);
        } catch(QueueDoesNotExistException e) {
            return null;
        }
    }

    public void close() {
        client.close();
    }

}
