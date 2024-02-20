package ch.elca.rovl;

import java.util.ArrayList;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class VoteProcessor implements Processor {

    private final List<String> supportedIde = List.of(
            "vscode",
            "eclipse",
            "intellij",
            "bluej",
            "netbeans",
            "other");

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class).toLowerCase();

        List<Integer> firstOccurrences = new ArrayList<>();
        int min = Integer.MAX_VALUE;

        for (int i = 0; i < supportedIde.size(); i++) {
            int firstOccurrence = body.indexOf(supportedIde.get(i));
            if (firstOccurrence < 0) {
                firstOccurrences.add(Integer.MAX_VALUE);
            } else {
                firstOccurrences.add(firstOccurrence);
                if (firstOccurrence < min) {
                    min = firstOccurrence;
                }
            }
        }

        if (min == Integer.MAX_VALUE) {
            exchange.getIn().setBody("unrecognized");
        } else {
            int idx = firstOccurrences.indexOf(min);
            exchange.getIn().setBody(supportedIde.get(idx));
        }
    }

}
