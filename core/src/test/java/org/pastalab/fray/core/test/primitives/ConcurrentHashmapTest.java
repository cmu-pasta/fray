package org.pastalab.fray.core.test.primitives;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.junit.jupiter.api.Test;
import org.pastalab.fray.core.TestRunner;
import org.pastalab.fray.core.command.Configuration;
import org.pastalab.fray.core.command.ExecutionInfo;
import org.pastalab.fray.core.command.LambdaExecutor;
import org.pastalab.fray.core.observers.ScheduleRecorder;
import org.pastalab.fray.core.observers.ScheduleRecording;
import org.pastalab.fray.core.randomness.ControlledRandom;
import org.pastalab.fray.core.scheduler.FifoScheduler;
import org.pastalab.fray.core.test.FrayRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ConcurrentHashmapTest extends FrayRunner {

    public static class DeterministicHashCodeObject {
        private final int hashCode;
        public DeterministicHashCodeObject(int hashCode) {
            this.hashCode = hashCode;
        }
        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    @Test
    public void testConcurrentHashMapDifferentExecutionPath() {
        /**
         * ConcurrentHashMap uses the hash code of the key to determine the bucket in which to place the key-value pair.
         * This introduces non-determinism in the order of insertion of key-value pairs.
         * We need to make sure Fray can handle this.
         */
        Function0<Unit> task = () -> {
            ConcurrentHashMap<Object, Integer> map = new ConcurrentHashMap<>();
            AtomicInteger i = new AtomicInteger(0);
            map.put(new DeterministicHashCodeObject(1260025044), i.getAndIncrement());
            map.put(new DeterministicHashCodeObject(1308867306), i.getAndIncrement());
            map.put(new DeterministicHashCodeObject(1014958949), i.getAndIncrement());
            return null;
        };
        TestRunner runner = buildRunner(task, new FifoScheduler(), 1, new ControlledRandom());
        ScheduleRecorder recorder = new ScheduleRecorder();
        runner.getConfig().getScheduleObservers().add(recorder);
        runner.run();
        List<ScheduleRecording> recordings1 = new ArrayList<>(recorder.getRecordings());


        task = () -> {
            ConcurrentHashMap<Object, Integer> map = new ConcurrentHashMap<>();
            AtomicInteger i = new AtomicInteger(0);
            map.put(new DeterministicHashCodeObject(-1889127028), i.getAndIncrement());
            map.put(new DeterministicHashCodeObject(1906549723), i.getAndIncrement());
            map.put(new DeterministicHashCodeObject(-507498936), i.getAndIncrement());
            return null;
        };
        TestRunner runner2 = buildRunner(task, new FifoScheduler(), 1, new ControlledRandom());
        ScheduleRecorder recorder2 = new ScheduleRecorder();
        runner2.getConfig().getScheduleObservers().add(recorder2);
        runner2.run();
        List<ScheduleRecording> recordings2 = new ArrayList<>(recorder2.getRecordings());
        assertNotEquals(recordings1, recordings2);
    }

    @Test
    public void testConcurrentHashMapSameExecutionPath() {
        for (int iter = 0; iter < 100; iter++) {
            Function0<Unit> task = () -> {
                ConcurrentHashMap<Object, Integer> map = new ConcurrentHashMap<>();
                AtomicInteger i = new AtomicInteger(0);
                Object o = new Object();
                System.out.println(o.hashCode());
                map.put(o, i.getAndIncrement());
                o = new Object();
                System.out.println(o.hashCode());
                map.put(o, i.getAndIncrement());
                o = new Object();
                System.out.println(o.hashCode());
                map.put(o, i.getAndIncrement());
                return null;
            };
            TestRunner runner = buildRunner(task, new FifoScheduler(), 1, new ControlledRandom());
            ScheduleRecorder recorder = new ScheduleRecorder();
            runner.getConfig().getScheduleObservers().add(recorder);
            runner.run();
            ControlledRandom randomSource = runner.getConfig().getRandomnessProvider();
            List<ScheduleRecording> recordings1 = new ArrayList<>(recorder.getRecordings());


            task = () -> {
                ConcurrentHashMap<Object, Integer> map = new ConcurrentHashMap<>();
                AtomicInteger i = new AtomicInteger(0);
                Object o = new Object();
                System.out.println(o.hashCode());
                map.put(o, i.getAndIncrement());
                o = new Object();
                System.out.println(o.hashCode());
                map.put(o, i.getAndIncrement());
                o = new Object();
                System.out.println(o.hashCode());
                map.put(o, i.getAndIncrement());
                return null;
            };
            TestRunner runner2 = buildRunner(task, new FifoScheduler(), 1, new ControlledRandom());
            runner2.getConfig()
                    .setRandomnessProvider(new ControlledRandom(randomSource.getIntegers(),
                            randomSource.getDoubles(), new Random()));
            ScheduleRecorder recorder2 = new ScheduleRecorder();
            runner2.getConfig().getScheduleObservers().add(recorder2);
            runner2.run();
            List<ScheduleRecording> recordings2 = new ArrayList<>(recorder2.getRecordings());
            assertEquals(recordings1, recordings2);
        }
    }

}
