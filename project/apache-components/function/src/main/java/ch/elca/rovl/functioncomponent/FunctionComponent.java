package ch.elca.rovl.functioncomponent;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.rovl.functioncomponent.util.PropertyResolver;
import ch.elca.rovl.functioncomponent.util.TargetProvider;
import ch.elca.rovl.functioncomponent.util.TriggerType;

@Component("function")
public class FunctionComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(FunctionComponent.class);
    private static final int MIN_PORT_NUMBER = 49152;
    private static final int MAX_PORT_NUMBER = 65534;

    final Map<String, DirectFunctionConsumer> consumers = new HashMap<>();
    final Random rng = new Random();

    public FunctionComponent() {
    }

    public FunctionComponent(CamelContext context) {
        super(context);
    }

    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        LOG.info("Creating function edpoint with name " + remaining + " and parameters " + parameters.toString());

        String providerProperty = String.format("function.%s.provider", remaining);

        TargetProvider provider = PropertyResolver.getTargetProvider(
                getCamelContext().getPropertiesComponent().resolveProperty(
                        providerProperty));

        if (provider == null) {
            LOG.info("No provider settings found.");

            TriggerType triggerType = PropertyResolver
                    .getTriggerType(getCamelContext().getPropertiesComponent().resolveProperty(
                            "function.trigger"));
            Optional<String> fnName = getCamelContext().getPropertiesComponent().resolveProperty(
                "function.name");
            if (fnName.isEmpty()) {
                throw new IllegalStateException(
                    "Missing configuration 'function.name' for debugging.");
            }

            if (triggerType != null) {
                LOG.info("Trigger configuration found: " + triggerType);

                if (triggerType == TriggerType.API) {
                    // open localhost websocket with netty-http
                    int port;
                    do {
                        port = rng.nextInt(MAX_PORT_NUMBER - MIN_PORT_NUMBER) + MIN_PORT_NUMBER;
                    } while (!isPortAvailable(port));
                    String apiUri = "localhost:" + port + "/" + fnName.get();
                    return getCamelContext().getEndpoint("netty-http:http://" + apiUri);
                } else if (triggerType == TriggerType.HTTP) {
                    // subscribe to quarkus kafka topic
                    LOG.info("Creating kafka endpoint: " + "kafka:" + fnName.get());
                    return getCamelContext().getEndpoint("kafka:" + fnName.get());        
                } else {
                    throw new IllegalStateException("");
                }
            } else {
                throw new IllegalStateException("Missing configuration 'function.trigger' for debugging.");
            }

        } else {
            return new FunctionEndpoint(uri, remaining, this);
        }
    }

    public void addConsumer(String functionName, DirectFunctionConsumer consumer) {
        synchronized (consumers) {
            if (consumers.putIfAbsent(functionName, consumer) != null) {
                throw new IllegalArgumentException(String.format(
                        "Cannot add a second consumer to the same endpoint for function '%s'", functionName));
            }

            consumers.notifyAll();
        }
    }

    public DirectFunctionConsumer getConsumer(String functionName) throws InterruptedException {
        synchronized (consumers) {
            DirectFunctionConsumer consumer = consumers.get(functionName);

            if (consumer == null) {
                // try a second time after a while
                consumers.wait(1000);
                consumer = consumers.get(functionName);

                if (consumer == null) {
                    throw new IllegalStateException(String.format(
                            "Failed to retrieve direct consumer for function '%s'", functionName));
                }
            }

            return consumer;
        }
    }

    /**
     * Checks to see if a specific port is available.
     *
     * @param port the port to check for availability
     */
    private static boolean isPortAvailable(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }
}
