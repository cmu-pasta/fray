package org.pastalab.fray.runtime;

abstract public class SyncurityCondition {
    abstract public boolean satisfied();
    public void await() {
        Runtime.onSyncurityCondition(this);
    }

    /**
     * A callback to notify [RunContext] that we are going to evaluate
     * the syncurity condition.
     */
    public void syncurityConditionEvaluationStart() {
        Runtime.onSyncurityConditionEvaluationStart();
    }

    /**
     * A callback to notify [RunContext] that we have finished
     * evaluating the syncurity condition.
     */
    public void syncurityConditionEvaluationEnd() {
        Runtime.onSyncurityConditionEvaluationDone();
    }
}
