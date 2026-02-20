import org.junit.Test;

import static org.junit.Assert.assertEquals;
import org.pastalab.fray.core.TestRunner;
import org.pastalab.fray.core.command.Configuration;
import org.pastalab.fray.core.command.ExecutionInfo;
import org.pastalab.fray.core.command.MethodExecutor;
import org.pastalab.fray.core.command.NetworkDelegateType;
import org.pastalab.fray.core.command.SystemTimeDelegateType;
import org.pastalab.fray.core.randomness.ControlledRandomProvider;
import org.pastalab.fray.core.scheduler.PCTScheduler;
import org.pastalab.fray.junit.Common;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SimpleJunit4Tests {

  @Test
  public void testAddition() {
    String classpath = System.getProperty("java.class.path");
    List<String> classpaths = Arrays.stream(classpath.split(File.pathSeparator)).toList();
    Configuration config = new Configuration(
        new ExecutionInfo(
            new MethodExecutor(
                "SimpleJunit4Tests",
                "simpleConcurrencyTest",
                new ArrayList<>(),
                classpaths,
                new HashMap<>()
            ),
            false,
            false,
            -1
        ),
        Common.INSTANCE.getWORK_DIR(),
        1000,
        600,
        new PCTScheduler(),
        new ControlledRandomProvider(),
        true,
        false,
        true,
        false,
        false,
        false,
        NetworkDelegateType.PROACTIVE,
        SystemTimeDelegateType.NONE,
        100_000L,
        true,
        true,
        true,
        false,
        false
        );
    TestRunner runner = new TestRunner(config);
    runner.run();
  }

  public void simpleConcurrencyTest() throws InterruptedException {
    int a = 1;
    int b = 2;
    int c = a + b;
    Thread t1 = new Thread(() -> {
      int d = c * 2;
      assertEquals(6, d);
    });

    t1.start();
    t1.join();
  }
}
