package ch.elca.rovl.dsl.api.function.models.functiontrigger.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.elca.rovl.dsl.api.function.models.functiontrigger.FunctionTrigger;
import ch.elca.rovl.dsl.resource.function.AuthorizationType;
import ch.elca.rovl.dsl.resource.function.HttpMethod;
import ch.elca.rovl.dsl.resource.function.TriggerType;

public class FunctionHttpTrigger extends FunctionTrigger {

    List<HttpMethod> supportedMethods;
    AuthorizationType authType;

    private FunctionHttpTrigger(List<HttpMethod> supportedMethods, AuthorizationType authorizationType) {
        super(TriggerType.REST);

        this.supportedMethods = supportedMethods;
        this.authType = authorizationType;
    }

    public static FunctionHttpTriggerBuilder builder() {
        return new FunctionHttpTriggerBuilder();
    }

    public static FunctionHttpTrigger standard() {
        List<HttpMethod> methods = new ArrayList<>();
        methods.add(HttpMethod.POST);

        return new FunctionHttpTrigger(methods, AuthorizationType.PUBLIC);
    }

    public List<HttpMethod> getHttpMethods() { return supportedMethods; }
    public AuthorizationType getAuthType() { return authType; }


    public static class FunctionHttpTriggerBuilder {

        List<HttpMethod> supportedMethods;
        AuthorizationType authType;

        public FunctionHttpTriggerBuilder() {
            supportedMethods = new ArrayList<>();
            authType = AuthorizationType.PUBLIC;
        }

        public FunctionHttpTriggerBuilder withHttpMethods(HttpMethod... methods) {
            supportedMethods.addAll(Arrays.asList(methods));
            return this;
        }

        public FunctionHttpTriggerBuilder withAuthorization(AuthorizationType method) {
            authType = method;
            return this;
        }

        public FunctionHttpTrigger build() {
            return new FunctionHttpTrigger(supportedMethods, authType);
        }


    }
}
