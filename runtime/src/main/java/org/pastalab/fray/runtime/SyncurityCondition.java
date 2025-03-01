package org.pastalab.fray.runtime;

abstract public class SyncurityCondition {
    public static int NEXT_ID = 0;

    public SyncurityCondition() {
        id = NEXT_ID++;
    }

    public int id;

    abstract public boolean satisfied();
    public void await() {
        Runtime.onSyncurityCondition(this);
    }
}
