package org.pastalab.fray.core.test;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.pastalab.fray.core.TestRunner;
import org.pastalab.fray.core.command.Configuration;
import org.pastalab.fray.core.command.ExecutionInfo;
import org.pastalab.fray.core.command.LambdaExecutor;
import org.pastalab.fray.core.randomness.ControlledRandom;
import org.pastalab.fray.core.scheduler.FifoScheduler;
import org.pastalab.fray.core.scheduler.POSScheduler;
import org.pastalab.fray.core.scheduler.Scheduler;


public class FrayRunner {
    public Throwable runWithFifo(Function0<Unit> exec) {
        return runWithScheduler(exec, new FifoScheduler(), 1);
    }

    public Throwable runWithPOS(Function0<Unit> exec) {
        return runWithScheduler(exec, new POSScheduler(), 10000);
    }

    public Throwable runWithScheduler(Function0<Unit> exec, Scheduler scheduler, int iter) {
        return buildRunner(exec, scheduler, iter).run();
    }

    public TestRunner buildRunner(Function0<Unit> exec, Scheduler scheduler, int iter) {
        Configuration config = new Configuration(
                new ExecutionInfo(
                        new LambdaExecutor(() -> {
                            exec.invoke();
                            return null;
                        }),
                        false,
                        true,
                        false,
                        -1
                ),
                "/tmp/report",
                iter,
                scheduler,
                new ControlledRandom(),
                true,
                false,
                true,
                false,
                false,
                false
        );
        TestRunner runner = new TestRunner(config);
        return runner;
    }
}
