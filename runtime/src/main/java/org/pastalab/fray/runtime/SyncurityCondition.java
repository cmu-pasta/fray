package org.pastalab.fray.runtime;

abstract public class SyncurityCondition {
    abstract public boolean satisfied();
    public void await() {
        Runtime.onSyncurityCondition(this);
    }
}
