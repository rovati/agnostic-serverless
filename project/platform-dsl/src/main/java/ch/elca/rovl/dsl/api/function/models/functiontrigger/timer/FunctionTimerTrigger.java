package ch.elca.rovl.dsl.api.function.models.functiontrigger.timer;

import ch.elca.rovl.dsl.api.function.models.functiontrigger.FunctionTrigger;
import ch.elca.rovl.dsl.resource.function.TriggerType;

public class FunctionTimerTrigger extends FunctionTrigger {

    IntervalPeriod period;

    private FunctionTimerTrigger(IntervalPeriod period) {
        super(TriggerType.TIMER);
        this.period = period;
    }

    public static FunctionTimerTrigger withPeriod(IntervalPeriod period) {
        return new FunctionTimerTrigger(period);
    }

    public static class IntervalPeriod {

        Kind kind;
        int quantity;

        private IntervalPeriod(Kind kind, int quantity) {
            this.kind = kind;
            this.quantity = quantity;
        }

        public static IntervalPeriod milliseconds(int ms) {
            return new IntervalPeriod(Kind.MILLIS, ms);
        }

        public static IntervalPeriod seconds(int s) {
            return new IntervalPeriod(Kind.SECS, s);
        }

        public static IntervalPeriod minutes(int m) {
            return new IntervalPeriod(Kind.MINS, m);
        }

        public static IntervalPeriod hours(int h) {
            return new IntervalPeriod(Kind.HOURS, h);
        }

        private enum Kind {
            MILLIS,
            SECS,
            MINS,
            HOURS
        }
    }
    
}
