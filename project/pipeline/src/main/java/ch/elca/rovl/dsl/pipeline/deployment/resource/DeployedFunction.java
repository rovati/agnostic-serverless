package ch.elca.rovl.dsl.pipeline.deployment.resource;

import java.util.ArrayList;
import java.util.List;

import com.azure.resourcemanager.appservice.models.FunctionApp;
import ch.elca.rovl.dsl.pipeline.templating.resource.DeployableFunction;
import ch.elca.rovl.dsl.pipeline.util.RequiredData;

/**
 * Object representing a deployed cloud function with its target provider chosen by the user and the
 * name of the corresponding resource in the cloud. It also contains a list of needed additional
 * configuration data.
 */
public class DeployedFunction extends DeployedResource {

    final List<RequiredData> requiredDataList;
    // TODO change this into something better
    FunctionApp functionApp;

    public DeployedFunction(DeployableFunction fn) {
        super(fn);
        this.requiredDataList = new ArrayList<>();
    }

    public DeployedFunction(DeployableFunction fn, List<RequiredData> requiredData) {
        super(fn);
        this.requiredDataList = requiredData;
    }

    public DeployableFunction getFunction() {
        return (DeployableFunction) deployableResource;
    }

    /**
     * Sets the Azure FunctionApp containing this function.
     * 
     * @param fnapp
     */
    public void setFunctionApp(FunctionApp fnapp) {
        this.functionApp = fnapp;
    }

    public void addRequiredData(RequiredData requiredData) {
        this.requiredDataList.add(requiredData);
    }

    public void addRequiredData(List<RequiredData> requiredData) {
        this.requiredDataList.addAll(requiredData);
    }

    public boolean requiresData() {
        return requiredDataList.size() > 0;
    }

    public List<RequiredData> getRequiredData() {
        return requiredDataList;
    }

    public FunctionApp getFunctionApp() {
        return functionApp;
    }

    public String getCloudName() {
        return getFunction().getCloudName();
    }

    public boolean requiresGlue() {
        return getFunction().getFunction().requiresGlue();
    }

    @Override
    public String toString() {
        return String.format("DeployedFunction[name=%s, cloud-name=%s, provider=%s]", getName(),
                getCloudName(), getProvider());
    }

}
