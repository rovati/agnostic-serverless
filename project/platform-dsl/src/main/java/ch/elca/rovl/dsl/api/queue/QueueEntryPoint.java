package ch.elca.rovl.dsl.api.queue;

import ch.elca.rovl.dsl.registry.SystemRegistry;

/**
 * Entry point for the fluent configuration of a queue resource.
 * <p>
 * NOTE the queue building block has no required field other than the unique
 * name. Optional config values are provided directly through this class instead
 * of dedicated steps.
 */
public class QueueEntryPoint {

    FluentQueueBuilder fqb;

    public QueueEntryPoint(SystemRegistry systemRegistry, String queueName) {
        this.fqb = new FluentQueueBuilder(queueName);
        systemRegistry.registerQueue(queueName, fqb);
    }

    /**
     * Configures the max size in megabytes of the queue.
     * 
     * @param size size in MB
     * @return this
     */
    public QueueEntryPoint maxSizeMB(int size) {
        this.fqb.withMaxQueueSizeMB(size);
        return this;
    }

}
