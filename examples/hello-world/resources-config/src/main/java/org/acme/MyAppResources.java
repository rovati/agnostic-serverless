package org.acme;

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
        function("helloworldfn")
            .pathToProject("../functions/")
            .runtime(FunctionRuntime.java(JavaVersion.V_17))
            .handler("org.acme.FunctionRoute")
            .rest(FunctionHttpTrigger.builder()
                .withAuthorization(AuthorizationType.PUBLIC)
                .withHttpMethods(HttpMethod.POST)
                .build());
    }
    
}
