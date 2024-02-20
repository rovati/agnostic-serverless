package ch.elca.rovl.dsl.pipeline.deployment.accessinfo.queue;

/**
 * Object containing access information for a queue on Azure.
 */
public class AzureQueueAccess extends QueueAccess {
    final String sendSAS;
    final String receiveSAS;

    public AzureQueueAccess(String sendSAS, String receiveSAS) {
        this.sendSAS = sendSAS;
        this.receiveSAS = receiveSAS;
    }

    public String getSendSAS() { return sendSAS; }
    public String getReceiveSAS() { return receiveSAS; }
}
