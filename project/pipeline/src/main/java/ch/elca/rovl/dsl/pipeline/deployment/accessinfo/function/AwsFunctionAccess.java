package ch.elca.rovl.dsl.pipeline.deployment.accessinfo.function;

public final class AwsFunctionAccess extends FunctionAccess {
    
    String lambdaArn;

    public void setLambdaArn(String lambdaArn) {
        this.lambdaArn = lambdaArn;
    }

    public String getLambdaArn() {
        return lambdaArn;
    }
}
