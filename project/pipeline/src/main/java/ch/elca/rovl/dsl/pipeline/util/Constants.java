package ch.elca.rovl.dsl.pipeline.util;

/**
 * Class containing various constant values used in the pipeline.
 */
public class Constants {

    /**
     * Name of the file containing the provider choice for each resource defined by the user.
     */
    public final static String PROVIDERS_FILENAME = "providers.properties";
    /**
     * Path of the directory where all generated code will be written to.
     */
    public final static String GENERATED_DIR = "generated/";
    /**
     * Path of the directory where all generated logs will be written to.
     */
    public final static String LOGS_DIR = GENERATED_DIR + "logs/";
    /**
     * Path of the directory where all generated debugging data will be written to.
     */
    public final static String DEBUGGING_DIR = GENERATED_DIR + "debugging/";
    /**
     * Path of the directory where all generated deployment data will be written to.
     */
    public final static String DEPLOYMENT_DIR = GENERATED_DIR + "deployment/";
    /**
     * Name of the file where names of resources are written to facilitate recognition of resources
     * that have already been deployed.
     */
    public final static String DEPLOYMENT_MEMORY_FILE =
            "deployment-info/resource-names.txt";

}
