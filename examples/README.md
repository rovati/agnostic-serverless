# Examples

This directory contains a collection of examples using the proposed solution. The examples are meant to showcase some simple usage and act as refernce for developers exploring the capabilities of the project.

| Project name | Description |
| ------------ | ----------- |
| [hello-world](https://github.com/rovati/agnostic-serverless/tree/main/examples/hello-world) | Simple serverless application consisting of a single REST-triggered function |
| [reference-app](https://github.com/rovati/agnostic-serverless/tree/main/examples/reference-app) | Serverless application used as back-end for a favourite IDE voting poll |


## hello-world

Simple application that replies with a "Hello World!" body to HTTP POST requests. The application contains configuration and implementation of a single function.

## reference-app

This application implements the example used through the thesis report. The app collects votes for the community favourite Java IDE. It consists of four functions:

1. A function that is triggerable by HTTP GET requests and replies with the current results of the poll stored in a database
2. A function that accepts HTTP POST requests to cast a vote for an IDE. This function extracts the IDE voted from the request body and passes it to a queue
3. A function that is triggered by the queue and updates the database containing the results
4. A utility function that initializes the database with some initial schema and values

Other than the functions, the application integrates the following platform resources:

1. A queue that collects votes
2. A database that stores the results of the poll