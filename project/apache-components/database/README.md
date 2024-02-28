# database

This cusotm Camel component offers a platform-agnostic interface to interact with relational databases (currently supports only PostgreSQL 13 as engine).

Its implementation merely acts as a proxy for `camel-jdbc` components. The latter is used to interact with the databases.

The reason why a custom database component has still been implemented is to keep consistency in the solution model. This way, database building blocks are integrated using `database:databaseName` URIs, following the same schema as the function and queue components.

## Using the component

Install the component in the local Maven repository by running `mvn install` in this directory. Then, add it as Maven dependency in the `pom.xml` of your project:

```xml
<dependency>
    <groupId>ch.elca.rovl.agnostic-serverless</groupId>
    <artifactId>camel-database</artifactId>
    <version>1.2.0</version>
</dependency>
```

You can then use database endpoints in Camel route configurations using the following URI:

`database:databaseName`

The component provides only producer endpoints.