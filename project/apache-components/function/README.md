# function

This custom Camel component offers a platform-agnostic interface to interact with functions.

The implementation supports consumer endpoints that simply pick up events from the automatically generated function handlers and push them to the Camel route defined by the developer. It also supports producer endpoints to forward processed events to other serverless functions.

The producer uses HTTP POST requests to send the event to the target function. It waits for a response before completing the execution.

## Using the component

Install the component in the local Maven repository by running `mvn install` in this directory. Then, add it as Maven dependency in the `pom.xml` of your project:

```xml
<dependency>
    <groupId>ch.elca.rovl.agnostic-serverless</groupId>
    <artifactId>camel-function</artifactId>
    <version>1.1.0</version>
</dependency>
```

You can then use function producer endpoints in Camel route configurations using the following URI:

`function:functionName`

Consumer endpoints have to use the following URI:

`function:trigger`

in order to pick up events from the generated function handler.

The endpoints do not support any kind of configuration.