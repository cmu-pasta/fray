package org.pastalab.fray.runtime;

abstract public class RangerCondition {
    public static int NEXT_ID = 0;

    public RangerCondition() {
        id = NEXT_ID++;
    }

    public int id;

    abstract public boolean satisfied();
    public void await() {
        Runtime.onRangerCondition(this);
    }
}
