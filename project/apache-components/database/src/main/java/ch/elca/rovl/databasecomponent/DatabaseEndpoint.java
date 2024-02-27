package ch.elca.rovl.databasecomponent;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;

/**
 * Placeholder database endpoint. Not Used.
 *
 */
@UriEndpoint(firstVersion = "1.0.0", scheme = "database", title = "database", syntax="database:name",
             category = {Category.CLOUD})
public class DatabaseEndpoint extends DefaultEndpoint {
    @UriPath @Metadata(required = true)
    private String name;

    public DatabaseEndpoint() {
    }

    public DatabaseEndpoint(String uri, DatabaseComponent component) {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception {
        throw new UnsupportedOperationException("Not used. Dummy class.");
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("Not used. Dummy class.");
    }

    /**
     * Some description of this option, and what it does
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
