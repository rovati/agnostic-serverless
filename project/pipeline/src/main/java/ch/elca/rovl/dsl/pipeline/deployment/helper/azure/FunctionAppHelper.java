package ch.elca.rovl.dsl.pipeline.deployment.helper.azure;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.azure.resourcemanager.appservice.models.FunctionApp;
import com.azure.resourcemanager.appservice.models.FunctionApps;
import ch.elca.rovl.dsl.pipeline.deployment.DeploymentConstants;
import ch.elca.rovl.dsl.pipeline.util.Constants;

public class FunctionAppHelper {
    final FunctionApps client;
    final String deployCmd = " & mvn clean package & mvn azure-functions:deploy";

    public FunctionAppHelper(FunctionApps functionApps) {
        this.client = functionApps;

        // create log dir if it dosen't exist
        File logDirectory = new File(Constants.LOGS_DIR);
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }
    }

    /**
     * Deploys the function to azure, reusing its FunctionApp if it already exists.
     * NOTE: uses maven azure-functions plugin instead of sdk
     * 
     * @param pathToFunctionDir path to the root directory of the Azure Function project
     * @param functionName
     */
    public void deployFunction(String pathToFunctionDir, String functionName) {
        Process p;
        int cmdResult;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "cmd", "/c", "cd " + pathToFunctionDir, deployCmd);
            pb.redirectOutput(new File(Constants.LOGS_DIR + "deployment-" + functionName + ".txt"));

            p = pb.start();
            cmdResult = p.waitFor();
            if (cmdResult != 0) {
                throw new Error("Maven archetype generation for azure-functions failed.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(String.format(
                "Failed to run deployment command for function '%s'", functionName));
        }
    }

    /**
     * Returns a FunctionApp given its name
     * @param functionAppName
     * @return the FunctionApp
     */
    public FunctionApp getFunctionApp(String functionAppName) {
        return client.getByResourceGroup(
            DeploymentConstants.AZURE_DEFAULT_RESOURCE_GROUP, functionAppName);
    }

    /**
     * Returns the url of the given function
     * @param functionName
     * @param functionAppName
     * @return the url of the function
     */
    public String getFunctionUrl(String functionName, String functionAppName) {
        FunctionApp fnapp = getFunctionApp(functionAppName);

        Object[] bindings = fnapp.getHostnameBindings().keySet().toArray();

        if (bindings.length < 1)
            throw new IllegalStateException("Created FunctionApp does not have hostname bindings.");

        return String.format(DeploymentConstants.AZURE_FUNCTION_URL_FORMAT, bindings[0], functionName);
    }

    /**
     * Updates the settings of the given function app with the given map of environment variables.
     * @param functionApp
     * @param settings
     * @return the updated FunctionApp
     */
    public FunctionApp updateFunctionAppSettings(FunctionApp functionApp, Map<String,String> settings) {
        return functionApp.update()
            .withAppSettings(settings)
            .apply();
    }

}
