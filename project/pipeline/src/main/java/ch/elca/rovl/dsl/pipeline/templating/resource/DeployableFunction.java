package ch.elca.rovl.dsl.pipeline.templating.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.elca.rovl.dsl.pipeline.linking.resource.LinkedFunction;
import ch.elca.rovl.dsl.pipeline.util.Provider;
import ch.elca.rovl.dsl.pipeline.util.RequiredData;
import ch.elca.rovl.dsl.pipeline.util.RequiredData.Type;
import ch.elca.rovl.dsl.resource.function.Function.FunctionConfigType;

/**
 * Object representing a cloud function with its target provider chosen by the user and the name of
 * the corresponding resource in the cloud. It contains a reference to the database object of the
 * previous pipeline step, and information needed after deployment.
 */
public final class DeployableFunction extends DeployableResource {

    final String generatedPath;
    final List<RequiredData> requiredData;
    boolean isGlue;
    Provider glueProvider;
    String glueName;

    public DeployableFunction(LinkedFunction fn, String generatedPath) {
        super(fn);
        this.generatedPath = generatedPath;
        this.requiredData = new ArrayList<>();
        this.isGlue = false;
    }

    /**
     * Registers information that will be needed to be configured after deployment.
     * 
     * @param type type of configuration
     * @param resourceName name of the resource containing the information needed
     */
    public void require(Type type, String resourceName) {
        requiredData.add(new RequiredData(type, resourceName));
    }

    public void setAsGlue(Provider glueProvider) {
        this.isGlue = true;
        this.glueProvider = glueProvider;
        this.glueName = super.getName() + "-glue";
    }

    public String getGeneratedPath() {
        return generatedPath;
    }

    public LinkedFunction getFunction() {
        return (LinkedFunction) linkedResource;
    }

    public List<RequiredData> getRequiredData() {
        return requiredData;
    }

    public boolean isGlue() {
        return isGlue;
    }

    public Map<FunctionConfigType, Object> getConfig() {
        return getFunction().getConfig();
    }

    @Override
    public String getName() {
        return isGlue ? glueName : super.getName();
    }

    @Override
    public Provider getProvider() {
        return isGlue ? glueProvider : super.getProvider();
    }

    @Override
    public String toString() {
        return String.format(
                "DeployableFunction[name=%s, provider=%s, cloud-name=%s]", getName(),
                getProvider(), cloudName);
    }
}
