package org.pastalab.fray.runtime;

public abstract class RangerCondition {
  public static int NEXT_ID = 0;

  public RangerCondition() {
    id = NEXT_ID++;
  }

  public int id;

  public abstract boolean satisfied();

  public void await() {
    Runtime.onRangerCondition(this);
  }
}
