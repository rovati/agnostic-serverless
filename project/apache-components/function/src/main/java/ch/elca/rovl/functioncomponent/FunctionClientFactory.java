package ch.elca.rovl.functioncomponent;

import ch.elca.rovl.functioncomponent.client.HttpSendClient;
import ch.elca.rovl.functioncomponent.client.SendClient;
import ch.elca.rovl.functioncomponent.util.TargetProvider;

public class FunctionClientFactory {

    public static SendClient createSendClient(String functionName, TargetProvider provider) {
        switch(provider){
            case AZURE:
            case AWS: {
                return new HttpSendClient(functionName);
            }
            default:
                throw new IllegalArgumentException("Unsupported provider!");
        }
    }

}