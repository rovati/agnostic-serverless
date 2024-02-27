package ch.elca.rovl.dsl.pipeline.linking.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ch.elca.rovl.dsl.api.function.models.functiontrigger.FunctionTrigger;
import ch.elca.rovl.dsl.pipeline.infraparsing.resource.DslFunction;
import ch.elca.rovl.dsl.pipeline.util.Provider;
import ch.elca.rovl.dsl.pipeline.util.TriggerType;
import ch.elca.rovl.dsl.resource.function.Function.FunctionConfigType;

/**
 * Object representing a cloud function with its target provider chosen by the user. It contains a
 * reference to the function object of the previous pipeline step, the type of action that triggers
 * the function and additional information of required configuration for the deployment.
 */
public final class LinkedFunction extends LinkedResource {

    final List<LinkedResource> output;
    TriggerType triggerType;
    boolean requiresGlue;
    LinkedQueue inputQueue;
    boolean hasDatabaseOutput;
    boolean hasFunctionOutput;
    boolean hasDatabaseOutputOnAWS;

    // TODO extend class?
    // Azure
    String resourceGroupName;
    String region;

    public LinkedFunction(DslFunction function, Provider provider) {
        super(function, provider);
        this.output = new ArrayList<>();
        this.triggerType = TriggerType.HTTP;
        this.requiresGlue = false;
        this.hasDatabaseOutput = false;
        this.hasDatabaseOutputOnAWS = false;
        this.hasFunctionOutput = false;
    }

    /**
     * Register the given resource as output of this function.
     * 
     * @param r output resource
     */
    public void addOutput(LinkedResource r) {
        output.add(r);
        if (r instanceof LinkedDatabase) {
            hasDatabaseOutput = true;
            if (r.getProvider() == Provider.AWS)
                hasDatabaseOutputOnAWS = true;
        } else if (r instanceof LinkedFunction) {
            hasFunctionOutput = true;
        }
    }

    /**
     * Set the given queue as input resource of this function.
     * 
     * @param q input queue
     */
    public void setInputQueue(LinkedQueue q) {
        inputQueue = q;
        // if the queue is on a different provider, indicate that the deployment of an additional
        // function is needed
        requiresGlue = this.provider != q.getProvider();
        triggerType = requiresGlue ? TriggerType.HTTP : TriggerType.QUEUE;
    }

    /**
     * Set configuration values needed for the deployment on Azure.
     * 
     * @param resourceGroupName
     * @param region
     */
    public void setAzureValues(String resourceGroupName, String region) {
        this.resourceGroupName = resourceGroupName;
        this.region = region;
    }

    public boolean hasDatabaseOutput() {
        return hasDatabaseOutput;
    }

    public boolean hasFunctionOutput() {
        return hasFunctionOutput;
    }

    public boolean hasDatabaseOutputOnAWS() {
        return hasDatabaseOutputOnAWS;
    }

    public DslFunction getFunction() {
        return (DslFunction) dslResource;
    }

    public boolean requiresGlue() {
        return requiresGlue;
    }

    public LinkedQueue getInputQueue() {
        return inputQueue;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public FunctionTrigger getTrigger() {
        return (FunctionTrigger) getFunction().getConfig().get(FunctionConfigType.TRIGGER);
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getRegion() {
        return region;
    }

    public List<LinkedResource> getOutput() {
        return output;
    }

    public Map<FunctionConfigType, Object> getConfig() {
        return getFunction().getConfig();
    }

    @Override
    public String toString() {
        return String.format("LinkedFunction[name=%s, provider=%s, trigger=%s]",
                dslResource.getName(), provider, triggerType);
    }

}
