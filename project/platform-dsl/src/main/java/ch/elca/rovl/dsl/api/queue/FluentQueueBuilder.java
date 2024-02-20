package ch.elca.rovl.dsl.api.queue;

import ch.elca.rovl.dsl.api.FluentResourceBuilder;
import ch.elca.rovl.dsl.resource.queue.Queue;

public class FluentQueueBuilder implements FluentResourceBuilder {

    String name;
    // ...

    protected FluentQueueBuilder(String name) {
        this.name = name;
    }

    protected FluentQueueBuilder withMaxQueueSizeMB(int size) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void validate() {
        if (name == null || name.isEmpty())
            throw new IllegalStateException("Queue is missing the mandatory field 'name'");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Queue build() {
        return new Queue(name);
    }
    
}
