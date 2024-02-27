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
import ch.elca.rovl.functioncomponent.util.PropertyResolver;
import ch.elca.rovl.functioncomponent.util.TargetProvider;
import ch.elca.rovl.functioncomponent.util.TriggerType;

/**
 * Camel component for function resources.
 * <p>
 * The endpoint URI is specified as "function:functionName". The consumer only
 * supports "trigger" as name.
 */
@Component("function")
public class FunctionComponent extends DefaultComponent {

    // ports range used for local debugging
    private static final int MIN_PORT_NUMBER = 49152;
    private static final int MAX_PORT_NUMBER = 65534;

    private final Map<String, DirectFunctionConsumer> consumers = new HashMap<>();
    private final Random rng = new Random();

    public FunctionComponent() {
    }

    public FunctionComponent(CamelContext context) {
        super(context);
    }

    /**
     * Creates an ednpoint for the given function. If no property indicating the
     * platform of the function exists, the endpoint is used for local debugging.
     */
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        // get property indicating on which platform the function is provisioned on
        String providerProperty = String.format("function.%s.provider", remaining);
        TargetProvider provider = PropertyResolver.getTargetProvider(
                getCamelContext().getPropertiesComponent().resolveProperty(
                        providerProperty));

        // if the property doesn't exist, the endpoint is being used for local debuggin
        if (provider == null) {
            // get property indicating whether the function creating this endpoint is
            // triggered by a REST call or by other functions
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
                // if triggered by REST, use endpoint to open a localhost port and listen
                if (triggerType == TriggerType.API) {
                    // get a free port
                    int port;
                    do {
                        port = rng.nextInt(MAX_PORT_NUMBER - MIN_PORT_NUMBER) + MIN_PORT_NUMBER;
                    } while (!isPortAvailable(port));
                    // create localhost url
                    String apiUri = "localhost:" + port + "/" + fnName.get();
                    // return netty-http endpoint
                    return getCamelContext().getEndpoint("netty-http:http://" + apiUri);
                } else if (triggerType == TriggerType.HTTP) {
                    // local debugging communication between function is simulated with kafka topics
                    return getCamelContext().getEndpoint("kafka:" + fnName.get());
                } else {
                    throw new IllegalStateException("Unsupported trigger type");
                }
            } else {
                throw new IllegalStateException("Missing configuration 'function.trigger' for debugging.");
            }
        } else {
            // otherwise return a normal function endpoint for cloud communication
            return new FunctionEndpoint(uri, remaining, this);
        }
    }

    /**
     * Registers a local consumer for discovery by local producers used by function
     * handlers
     * 
     * @param functionName
     * @param consumer
     */
    public void addConsumer(String functionName, DirectFunctionConsumer consumer) {
        synchronized (consumers) {
            if (consumers.putIfAbsent(functionName, consumer) != null) {
                throw new IllegalArgumentException(String.format(
                        "Cannot add a second consumer to the same endpoint for function '%s'", functionName));
            }

            consumers.notifyAll();
        }
    }

    /**
     * Returns a local consumer used by function handlers to push messages to the
     * Camel route.
     * 
     * @param functionName
     * @return
     * @throws InterruptedException
     */
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
     * Checks whether the given port is available.
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
