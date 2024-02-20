package ch.elca.rovl.layout;

import ch.elca.rovl.dsl.PlatformResourcesDefinition;
import ch.elca.rovl.dsl.api.function.models.FunctionRuntime;
import ch.elca.rovl.dsl.api.function.models.FunctionRuntime.JavaVersion;
import ch.elca.rovl.dsl.api.function.models.functiontrigger.rest.FunctionHttpTrigger;
import ch.elca.rovl.dsl.resource.function.AuthorizationType;
import ch.elca.rovl.dsl.resource.function.HttpMethod;

/**
 * User defined layout of the serverless application
 */
public class MyAppResources extends PlatformResourcesDefinition {

    @Override
    public void define() {
        // define configuration for the function: make it triggerable by http post
        function("hello-world-fn")
            .pathToProject("../functions/")
            .runtime(FunctionRuntime.java(JavaVersion.V_17))
            .rest(FunctionHttpTrigger.builder()
                .withAuthorization(AuthorizationType.PUBLIC)
                .withHttpMethods(HttpMethod.POST)
                .build());
    }
    
}
