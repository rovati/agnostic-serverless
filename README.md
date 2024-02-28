# agnostic-serverless

Repository for the Master thesis "Abstracting Proprietary Serverless Cloud Building Blocks for Easier Development and Compliance".

It includes the implementation of the proposed solution, plus some examples and templates. Each directory contains README files to further explain their content. 


## How to use the proposed solution

### Requirements

1. [Maven](https://maven.apache.org/)
2. Java JDK 17 (or GraalVM JDK 17)
3. [Quarkus CLI](https://quarkus.io/get-started/)
4. [AWS CLI](https://aws.amazon.com/cli/) and/or [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)
5. A docker engine (e.g. using [Docker Desktop](https://www.docker.com/products/docker-desktop/) or [Rancher Desktop](https://rancherdesktop.io/)) for local debugging.

It is also necessary to be logged in through the platform CLIs before running the pipeline for deployment.

### Use the solution in your project

The easiest way to use the solution is by following the guides of the READMEs in the [project](https://github.com/rovati/agnostic-serverless/tree/main/project) directory and then starting from the [blank template](https://github.com/rovati/agnostic-serverless/tree/main/templates/tpl-blank).

### Developing an application

Developing a sevrerless application using the proposed solution consists of two elements:

1. A resource configuration project where you define and configure your app resources. This project is also used to indicate on which platform you want each app resource to be deployed to.
2. Function projects implementing the behaviour of the app.

Resources configuration is carried out through the proposed fluent API. A configuration project must include a class extending `PlatformResourcesDefinition` and a main class to run the pipeline. Platform choices are indicated with a mandatory `providers.properties` file in the root directory of the configuration project.

Functions are implemented using the [Apache Camel Java DSL](https://camel.apache.org/manual/java-dsl.html) and the [proposed abstract building block Camel components](https://github.com/rovati/agnostic-serverless/tree/main/project/apache-components) to integrate app resources. Each function project must contain a class extending a `RouteBuilder`. **NOTE** the root directory of the function project must have the same name as the correpsonding function resource name in the configuration project.


### Deploying an application

To deploy an application, you can follow these steps:

1. Define the platform each resource should deployed to in the `providers.properties` file located in the root directory of the resources configuration project.
2. Log in with the platform CLIs.
3. Run the resources configuration project with the `-deploy` argument.

### Debugging an application

To debug locally the functions of your app, follow this workflow:

1. Start the docker engine.
2. Run the resources configuration project with the `-debug` argument. This is necessary only the first time you launch the debugging process, when there have been changes in the endpoints used in function Camel routes, or if you restarted the docker engine.
3. Optionally, run functions you do *not* want to debug with the `quarkus dev` command in the root directory of their project. This is helpful to fully run the application locally.
4. Run the function you want to debug with the `mvn quarkus:dev -Dsuspend` command in its root directory.
5. Connect a remote debugger to the debugging port exposed by Quarkus Dev.