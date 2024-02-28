# tpl-blank

Basic template to start off serverless development using the proposed solution. The tempalte is comprised of two elements:

1. Project to define and configure app resources
2. Blank function project

## [functions/hello-world-fn](https://github.com/rovati/agnostic-serverless/tree/main/templates/tpl-blank/functions/hello-world-fn)

Blank function project used to implement the behaviour of a function and the integration of other app components. The project contains dependencies for the itnegration of other functions, queues and database. Remove the unnecessary dependencies to reduce the function execution time.

## [resources-config](https://github.com/rovati/agnostic-serverless/tree/main/templates/tpl-blank/resources-config)

Project used to define and configure resources. It consists of a configuration Java class where resources are defined using the proposed fluent API, and a main class used to run the pipeline.

The [providers.properties](https://github.com/rovati/agnostic-serverless/blob/main/templates/tpl-blank/resources-config/providers.properties) file can be used to specify on which platofmr each resource should be deployed. The configuration can be done this way:

| Config entry | Description |
| ------------ | ----------- |
| default=\<platform> | Specifies that every resource without an explicitly assigned platform should be deploy to this platform |
| \<resource-name>=\<platform> | Specifies that this resource should be deployed to this platform |

The `default` key can be used multiple time, but only the last occurrence will be considered. The `<resource-name>` overrides the `default` key for that specific resource only.

Possible values for `<platform>` are : `aws` and `azure`.