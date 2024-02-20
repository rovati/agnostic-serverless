package ch.elca.rovl.dsl.pipeline.deployment.helper.azure.model;

public class ServiceBusQueueKeys {
    final String sendConnectionString;
    final String listenConnectionString;

    public ServiceBusQueueKeys(String sendKey, String listenKey) {
        this.sendConnectionString = sendKey;
        this.listenConnectionString = listenKey;
    }

    public String sendConnectionString() { return sendConnectionString; }
    public String listenConnectionString() { return listenConnectionString; }
}
