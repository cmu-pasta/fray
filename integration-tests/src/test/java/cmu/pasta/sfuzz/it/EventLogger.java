package cmu.pasta.sfuzz.it;

import cmu.pasta.sfuzz.core.concurrency.operations.Operation;
import cmu.pasta.sfuzz.core.logger.LoggerBase;
import cmu.pasta.sfuzz.core.runtime.AnalysisResult;
import cmu.pasta.sfuzz.core.scheduler.Choice;

public class EventLogger implements LoggerBase {
    StringBuilder sb = new StringBuilder();

    @Override
    public void executionStart() {
    }

    @Override
    public void newOperationScheduled(Operation op, Choice choice) {
    }

    @Override
    public void executionDone(AnalysisResult result) {

    }

    @Override
    public void applicationEvent(String event) {
        sb.append(event);
    }
}
