package ch.elca.rovl.dsl.api.queue;

import ch.elca.rovl.dsl.registry.SystemRegistry;

public class QueueEntryPoint {

    FluentQueueBuilder fqb;

    public QueueEntryPoint(SystemRegistry systemRegistry, String queueName) {
        this.fqb = new FluentQueueBuilder(queueName);
        systemRegistry.registerQueue(queueName, fqb);
    }

    // TODO remove and offer config directly here
    public ConfigurableQueueStep config() {
        return new ConfigurableQueueStep(fqb);
    }

}
