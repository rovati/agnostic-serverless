package org.acme.layout;

import ch.elca.rovl.dsl.PlatformResourcesDefinition;

/**
 * User defined layout of the serverless application
 */
public class MyAppResources extends PlatformResourcesDefinition {

    @Override
    public void define() {
        // TODO define configuration of app resources
        
        /* resource examples with minimum required configuration
        
        function("blank-fn")
            .pathToProject("../functions/")
            .runtime(FunctionRuntime.java(JavaVersion.V_17));

        queue("my-queue");

        database("my-database")
            .engine(DatabaseEngine.POSTRGESQL)
            .rootUser("postgres", "postgres");
        */

    }
    
}
