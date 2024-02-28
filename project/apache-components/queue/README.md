# queue

This cusotm Camel component offers a platform-agnostic interface to interact with queues (currently supports AWS SQS and Azure Service Bus).

The implementation provides consumer endpoints to receive events from automatically generated function handlers and push them to the Camel route configured by the developer. It also provides producer endpoints to push processed events to queues provisione don the supported platforms. To do so, it relies on platform service clients.

## Using the component

Install the component in the local Maven repository by running `mvn install` in this directory. Then, add it as Maven dependency in the `pom.xml` of your project:

```xml
<dependency>
    <groupId>ch.elca.rovl.agnostic-serverless</groupId>
    <artifactId>camel-queue</artifactId>
    <version>1.2.0</version>
</dependency>
```

You can then use queue endpoints in Camel route configurations using the following URI:

`queue:queueName`

The component supports both consumer and producer endpoints.