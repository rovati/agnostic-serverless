package ch.elca.rovl.layout;

import ch.elca.rovl.dsl.PlatformResourcesDefinition;
import ch.elca.rovl.dsl.api.function.models.FunctionRuntime;
import ch.elca.rovl.dsl.api.function.models.FunctionRuntime.JavaVersion;
import ch.elca.rovl.dsl.api.function.models.functiontrigger.rest.FunctionHttpTrigger;
import ch.elca.rovl.dsl.resource.database.DatabaseEngine;
import ch.elca.rovl.dsl.resource.database.WorkloadType;
import ch.elca.rovl.dsl.resource.database.Database.DatabaseSize;
import ch.elca.rovl.dsl.resource.function.AuthorizationType;
import ch.elca.rovl.dsl.resource.function.HttpMethod;

/**
 * User defined layout of the serverless application
 */
public class MyAppResources extends PlatformResourcesDefinition {

    @Override
    public void define() {
        function("postvote")
            .pathToProject("../functions/")
            .runtime(FunctionRuntime.java(JavaVersion.V_17))
            .handler("ch.elca.rovl.FunctionRoute")
            .rest(FunctionHttpTrigger.builder()
                .withAuthorization(AuthorizationType.PUBLIC)
                .withHttpMethods(HttpMethod.POST)
                .build());

        queue("votesqueue");

        function("countupdate")
            .pathToProject("../functions/")
            .runtime(FunctionRuntime.java(JavaVersion.V_17))
            .handler("ch.elca.rovl.FunctionRoute");

        database("votesdb")
            .engine(DatabaseEngine.POSTRGESQL)
            .rootUser("postgres", "jjfkcoEIj83d;sl")
            .workload(WorkloadType.DEV)
            .sizeGB(DatabaseSize.MIN);

        function("getvotes")
            .pathToProject("../functions/")
            .runtime(FunctionRuntime.java(JavaVersion.V_17))
        .handler("ch.elca.rovl.FunctionRoute")
            .rest(FunctionHttpTrigger.builder()
                .withAuthorization(AuthorizationType.PUBLIC)
                .withHttpMethods(HttpMethod.GET)
                .build());

        function("initdb")
            .pathToProject("../functions/")
            .runtime(FunctionRuntime.java(JavaVersion.V_17))
            .handler("ch.elca.rovl.FunctionRoute")
            .rest(FunctionHttpTrigger.builder()
                .withAuthorization(AuthorizationType.PUBLIC)
                .withHttpMethods(HttpMethod.POST)
                .build());
            
    }
    
}
