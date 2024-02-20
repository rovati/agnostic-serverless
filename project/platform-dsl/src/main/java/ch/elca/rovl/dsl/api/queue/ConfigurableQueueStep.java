package ch.elca.rovl.dsl.api.queue;

public class ConfigurableQueueStep {
    
    FluentQueueBuilder fqb;

    public ConfigurableQueueStep(FluentQueueBuilder fqb) {
        this.fqb = fqb;
    }

    public ConfigurableQueueStep maxQueueSizeMB(int size) {
        this.fqb.withMaxQueueSizeMB(size);
        return this;
    }

}
