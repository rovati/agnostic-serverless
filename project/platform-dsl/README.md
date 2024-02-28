# platform-dsl

This project implements the fluent API used to define and configure app resources.

## Using the DSL

The DSL is included in the [pipeline dependency](https://github.com/rovati/agnostic-serverless/blob/main/project/pipeline/README.md#using-the-pipeline). Resources configuration can be defined in a class extending the `PlatformResourcesDefinition` abstract class and overriding the `define()` method:

```java
public class MyAppResources extends PlatformResourcesDefinition {
    @Override
    public void define() {
        // resource definitions here
    }
}
```

The `PlatformResourcesDefinition` file gives access to three static methods to start the definition of a resource: `function(String functionName)`, `queue(String queueName)` and `database(String databaseName)`. These methods can be used to start the fluent chain of resource configuration.

## Resources

The fluent API currently supports the definition of resources from the building blocks of function, queue and database.

### Function configuration

| Field name | Type | Required | Description |
| ---------- | ---- | -------- | ----------- |
| pathToProject | String | yes | Relative or absolute path the parent directory containing the function project. |
| runtime | FunctionRuntime | yes | Indicates the runtime of the function. |
| handler | String | yes | Specifies the fully qualified name of the class containing the Camel route configuration of the function. **NOTE** do not include file extensions. |
| execTime | int | no | Sets the timeout delay for the function exeution. The default value uses the default of the target platform. |
| rest | FunctionTrigger | no | Configures support for an HTTP trigger. |

### Database configuration

| Field name | Type | Required | Description |
| ---------- | ---- | -------- | ----------- |
| engine | DatabaseEngine | yes | Specifies which engine the database should use. |
| rootUser | String, String | yes | Sets username and password for the root admin of the database. |
| workload | WorkloadType | no | Indicates the expected workload type to define sevrer computing capabilites hosting the database. Default values is WorkloadType.DEV, which uses the minimum capabilities supported by the platform. |
| sizeGB | int or DatabaseSize | no | Indicates the maximum size in gigabyte of the database. DatabaseSize.MIN and DatabaseSize.MAX can be used to set the size to the minimum or maximum supported by the target platform. |

### Queue configuration

| Field name | Type | Required | Description |
| ---------- | ---- | -------- | ----------- |
| maxSizeMB | int | no | Sets the maximum size in megabytes for the queue. |