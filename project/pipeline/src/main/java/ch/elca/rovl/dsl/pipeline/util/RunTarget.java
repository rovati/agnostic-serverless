package ch.elca.rovl.dsl.pipeline.util;

/**
 * List of exec modes of the pipeline
 */
public enum RunTarget {
    // parses resource configuration and generates function projects ready to be deployed
    VALIDATE,
    // generates config and provisions local databases necessary for local debugging
    DEBUG,
    // deploys the application to the chosen platforms
    DEPLOY
}
