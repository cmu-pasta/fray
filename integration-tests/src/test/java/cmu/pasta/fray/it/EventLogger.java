package cmu.pasta.fray.it;

import cmu.pasta.fray.core.concurrency.operations.Operation;
import cmu.pasta.fray.core.logger.LoggerBase;
import cmu.pasta.fray.core.runtime.AnalysisResult;
import cmu.pasta.fray.core.scheduler.Choice;

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
