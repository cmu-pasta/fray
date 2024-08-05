package org.pastalab.fray.it;

import org.pastalab.fray.core.concurrency.operations.Operation;
import org.pastalab.fray.core.logger.LoggerBase;
import org.pastalab.fray.core.scheduler.Choice;

public class EventLogger implements LoggerBase {
    StringBuilder sb = new StringBuilder();

    @Override
    public void executionStart() {
    }

    @Override
    public void newOperationScheduled(Operation op, Choice choice) {
    }

    @Override
    public void executionDone(boolean bugFound) {

    }

    @Override
    public void applicationEvent(String event) {
        sb.append(event);
    }

    @Override
    public void shutdown() {
    }
}
