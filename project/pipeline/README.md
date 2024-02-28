# pipeline

This project implements the support pipeline of the solution. The pipeline is implemented in sequential stages to simplify the extension of the pipeline behaviour. The pipeline currently supports deployment to Azure and AWS. Platform-specific code to support platform deployment is separated form the pipeline stage engines, so that support for additional platforms can be easily introduced.


## Using the pipeline

Use the pipeline in a project to define and configure your app resources. To do so, run `mvn install` in this directory to install the jar to the local maven repository. Add then the maven dependency to your project:

```xml
<dependency>
    <groupId>ch.elca.rovl.agnostic-serverless</groupId>
    <artifactId>pipeline</artifactId>
    <version>1.2.0</version>
</dependency>
```

The dependency include the fluent API used to configure resources. Your project needs a main class to be run. The main method has to initiallize the pipeline with the configuration class:

```java
public static void main( String[] args )
{
    PipelineEngine engine = new PipelineEngine(new MyAppResources());
    engine.run(args);
}
```

where `MyAppResources` has to be changed to the name of the class extending the `PlatformResourcesDefinition` class to define and configure resources. Moreover, the project has to contain a `providers.properties` files in the root directory of the project. In this file the developer has to specify the platform each app resource has to be deployed to.

The pipeline can then be run in three different modes:

| arg | Description |
| --- | ----------- |
| no arrgument | The pipeline is used to validate the configuration and generate functionproojects ready to be deployed to the chosen platforms. |
| -debug | The pipeline is run for debugging. Configuration needed for local debugging is generated. |
| -deploy | The pipeline is run for deployment. Resources are deployed to their chosen platforms. At the end of this execution the app is fully deployed, configured and ready to be used. |


## Stages

| Stage | Description |
| ----- | ----------- |
| [infraparsing](https://github.com/rovati/agnostic-serverless/tree/main/project/pipeline/src/main/java/ch/elca/rovl/dsl/pipeline/infraparsing) | The first stage of the pipeline, it parses the resources configuration file and validates it |
| [linking](https://github.com/rovati/agnostic-serverless/tree/main/project/pipeline/src/main/java/ch/elca/rovl/dsl/pipeline/linking) | Examines the funciton projects to deduce the flow of events between app resources. It also parses the providers configuration file so that it matches each defined resource with the chosen platform for its deployment |
| [debugging](https://github.com/rovati/agnostic-serverless/tree/main/project/pipeline/src/main/java/ch/elca/rovl/dsl/pipeline/debugging) | Step run only in debug mode, it generates configuration needed by Camel components to run locally, and it provisions local database instances |
| [templating](https://github.com/rovati/agnostic-serverless/tree/main/project/pipeline/src/main/java/ch/elca/rovl/dsl/pipeline/templating) | Generates deployable function projects |
| [deployment](https://github.com/rovati/agnostic-serverless/tree/main/project/pipeline/src/main/java/ch/elca/rovl/dsl/pipeline/deployment) | Provisions and deploys app resources to the chosen platforms, and finalizes platform resources configuration so that the sevrerless application is ready to be used

### infraparsing

The stage retrieves the resources defined and configured through the fluent API by the developer. It validates the configuration to make sure every required value has been provided and that each resource has a unique name. It then translates the fluent API resources to corresponding resources that can used thorughout the pipeline.

### linking

The stage copies and extends the projects of the function resources with a class used to extract the endpoints used in the Camel route configuration of the function. It then extracts the itnegrated endpoints and use that information to deduce links between app resources. It also parses the `providers.properties` file to assign the chosen plaform to each resource.

### debugging

Run only for the debugging mode. It extends function projects with configuration needed by the platform-agnostic Camel endpoints to successfully run locally. It also generaes dummy Quarkus projects to trigger Quarkus Dev Services and provision necessary local database instances for debugging.

### templating

Run only for validation and deployment. It copies the function projects, generates platform-specific function handlers and extends the projects with files necessary to pack and deploy the project to the target platform. In case "glue" functions are needed (e.g. when a function needs to pull events from a queue on a different platform), projects for them are generated. This step also tags pipeline resources indicating what additional configuration values the function needs after deployment to successfully integrate other app components. Resources are assigned a new name that is used for the corresponding deployed resource. A local memory file is queried to detect whether the resource has already been deployed previously. If it is the case, the cloud name of the previous deployment is re-used.

### Deployment

Run only for deployment. It provisions databases and queues on the chosen platforms. If a resource with the same cloud name already exists on the platform, provisioning is skipped and that resource is re-used by the pipeline. The stage then packs function projets, provisions functions on the target platforms and deploys the code. Finally, information about the deployed resources is collected and deployed functions are updated with data necessary to correctly integrate app components they interact with. At the end of this stage, the deployed app is fully functional and ready to use.