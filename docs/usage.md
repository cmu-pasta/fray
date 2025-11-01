# Usage Examples

Fray is a tool that helps you to test multithreaded code. It is especially useful when you want to test the behavior of your code under different thread interleavings. Here is an example of how to add Fray to a Gradle project.
You may find the complete source code [here](https://github.com/cmu-pasta/fray-examples/tree/main/fray-gradle-example)

## Gradle Configuration

Add the following plugin to your `build.gradle` file:

```kotlin
plugins {
    id("org.pastalab.fray.gradle") version "0.6.8"
}
```

## Class You Want to Test

Fray does not require any special annotations or modifications to your code. You can write your test code as usual. Here is an example of a simple class that you want to test:

```java
import java.util.concurrent.atomic.AtomicInteger;

public class BankAccount {
    public AtomicInteger balance = new AtomicInteger(1000);
    public void withdraw(int amount) {
        // Check if there is enough balance
        if (balance.get() >= amount) {
            // Deduct the amount
            balance.set(balance.get() - amount);
        }
    }
}
```

## Test Code


### Original Test Code

Here is an example of a test code that tests the `BankAccount` class without Fray:

```java
import org.junit.jupiter.api.Test;
public class BankAccountTest {
    public void myBankAccountTest() throws InterruptedException {
        BankAccount account = new BankAccount();
        Thread t1 = new Thread(() -> {
            account.withdraw(500);
            assert(account.balance.get() > 0);
        });
        Thread t2 = new Thread(() -> {
            account.withdraw(700);
            assert(account.balance.get() > 0);
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    @Test
    public void runTestUsingJunit() throws InterruptedException {
        myBankAccountTest();
    }
}
```

### Test Code with Fray

Here is an example of a test code that tests the `BankAccount` class with Fray:

```java
...
import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

@ExtendWith(FrayTestExtension.class)
public class BankAccountTest {
    public void myBankAccountTest() throws InterruptedException {
        ...
    }

    @ConcurrencyTest(
            iterations = 1000
    )
    public void runTestUsingFray() throws InterruptedException {
        myBankAccountTest();
    }
}
```

- First you need to add the `@ExtendWith(FrayTestExtension.class)` annotation to the test class so that Fray can run the test.
- Then you need to add the `@ConcurrencyTest` annotation to the test method. The `iterations` parameter specifies how many times the test method should be executed.
    - You may also specify scheduling algorithms and other parameters in the `@ConcurrencyTest` annotation. For more information, see the [ConcurrencyTest.kt](https://github.com/cmu-pasta/fray/blob/main/junit/src/main/kotlin/org/pastalab/fray/junit/junit5/annotations/ConcurrencyTest.kt#L18)

## Run the Test

### Run test from command line

You can run the test from the command line using the following command:

```shell
./gradlew frayTest
```

Fray will launch all tests annotated with `@ConcurrencyTest` and run them multiple times.

### Run test from IDE

If you are using an IDE, you can run the test as you would run any other JUnit test. 

## Reproduce a Failure

If a test fails, Fray will automatically generate a test case that reproduces the failure. 
Fray prints the path of the recording in the standard output. 

```txt
Bug found in iteration test runTestUsingFray() repetition 0 of 1000, you may find detailed report and replay files in PATH_TO_FRAY_REPORT
```

In the report folder, Fray logs the detailed information of the test failure in `fray.log` file.

```txt
2025-02-17 13:43:40 [INFO]: Error found at iter: 1, step: 19, Elapsed time: 22ms
2025-02-17 13:43:40 [INFO]: Error: java.lang.AssertionError
Thread: Thread[#20037,Thread-20003,5,main]
java.lang.AssertionError
	at BankAccountTest.lambda$myBankAccountTest$0(BankAccountTest.java:14)
	at java.base/java.lang.Thread.run(Thread.java:1583)

2025-02-17 13:43:40 [INFO]: The recording is saved to PATH_TO_FRAY_REPORT/recording
```

Fray provides **two ways** to reproduce a failure:

---

#### 1. Replay using recorded random choices

You can rerun the program with the **same scheduler** and the **recorded random choices** used in the failing execution.  
This technique is described in detail in [our NSDI paper](https://aoli.al/papers/fest-nsdi26.pdf).

By default, **Fray** uses this method to reproduce failures.  
Each report folder includes both the scheduler type and the random choices used during the run.  
To replay the failure, simply provide the path to the replay file—no need to specify the scheduler explicitly:

```java
@ConcurrencyTest(
        replay = "PATH_TO_FRAY_REPORT/recording"
)
```

#### 2. Replay using the exact thread interleaving

Alternatively, you can reproduce the failure by replaying the exact thread schedule observed during the original execution.
This requires that the schedule was recorded when the test was run.

To enable schedule recording, set the following system property: `-Dfray.recordSchedule=true`.

> [!NOTE]
> This approach is less reliable if your program contains other sources of nondeterminism (e.g., random number generators, I/O operations, etc.) that influence the execution path.

Once the schedule is recorded, configure your test to use the `ReplayScheduler`:
```java
@ConcurrencyTest(
    scheduler = ReplayScheduler.class,
    replay = "PATH_TO_FRAY_REPORT/recording"
)
```

## NixOS

Fray downloads Corretto JDK 25 and runs `ConcurrencyTest` with it by default. However, NixOS cannot run dynamically 
linked executables. To run Fray on NixOS, you can provide environment variable `JDK25_HOME` and Fray will use provided 
JDK 25 instead of downloading it.

```nix
packages = with pkgs; [
  ...
  javaPackages.compiler.openjdk25
];
shellHook = ''
  export JDK25_HOME="${pkgs.javaPackages.compiler.openjdk25.home}"
''
```

## Agent Mode

Fray also provides a Java agent that lets you run Fray with existing Java applications without using the Fray launcher. This is useful when your application is already running inside a deterministic environment (such as [Antithesis](https://antithesis.com)) and you only want Fray to control thread interleavings.

To use the agent, you can start from Fray's prebuilt Docker image.

```dockerfile
FROM ghcr.io/cmu-pasta/fray:0.6.8 as fray

COPY --from=fray /nix /nix
COPY --from=fray /opt/fray /opt/fray
```

After you have the image, run your application with the Fray agent:

- Replace the `java` command with the instrumented `/opt/fray/java-inst/bin/java`.
- If you use launchers such as Gradle or Maven, set `JAVA_HOME` to `/opt/fray/java-inst`.
- Add the following two agents:
  - `-javaagent:/opt/fray/libs/fray-core-FRAY_VERSION-SNAPSHOT-all.jar=FRAY_ARGS`
    - `FRAY_ARGS` are the same arguments you would pass to the Fray launcher, separated by colons (:).
    - For example, to use the `pos` scheduler and enable memory interleaving:
      ```
      -javaagent:/opt/fray/libs/fray-core-0.6.8-SNAPSHOT-all.jar=-m:--scheduler:pos
      ```
  - `-agentpath:/opt/fray/native-libs/libjvmti.so`


### Use Fray inside Antithesis

To fully leverage Antithesis's fuzzing capabilities, add the following argument to the Fray agent: `--randomness-provider:antithesis-sdk-random`. You can also set this system property to have Fray report errors through the Antithesis SDK: `-Dfray.antithesisSdk=true`.

> [!NOTE]  
> Ensure the Antithesis SDK is available on your application's classpath. Fray does not package the Antithesis SDK.
