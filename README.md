# Fray: General-Purpose Concurrency Testing 

[![Build](https://github.com/cmu-pasta/fray/actions/workflows/main.yml/badge.svg)](https://github.com/cmu-pasta/fray/actions/workflows/main.yml)


Fray is a general-purpose concurrency testing tool for Java applications. It performs deterministic concurrency testing using various testing algorithms.
Fray is designed to be easy to use and can be integrated into existing testing frameworks.

> [!TIP]
> Can't wait to try Fray? Clone the repository and run `./gradlew build && export PATH=$PATH:$(pwd)/bin`. 
> Then replace `java` with `fray` in your command line to run your application with Fray.

## Getting Started


Consider you have a bank account application (you may find the complete example in
https://github.com/cmu-pasta/fray-examples/blob/main/fray-gradle-example/src/test/java/BankAccountTest.java):

```java
public static class BankAccount {
    private AtomicInteger balance = new AtomicInteger();

    public BankAccount(int initialBalance) {
        this.balance.set(initialBalance);
    }

    // Transfer money from this account to another account
    public void transfer(BankAccount target, int amount) {
        if (this.balance.get() >= amount) {
            this.balance.set(this.balance.get() - amount);
            target.balance.set(target.balance.get() + amount);
        } else {
            System.out.println("Insufficient funds to transfer " + amount + " from " + this);
        }
    }
}
```
The bug here is in the logic of checking and updating the balance in the transfer method. The check 
`if (this.balance >= amount)` is done before the actual transfer, but the method doesn’t account for the fact that 
another thread might simultaneously perform a transfer.

You may write a test for the application using JUnit:

```java
public void testBankAccount() {
    BankAccount account1 = new BankAccount(1000);
    BankAccount account2 = new BankAccount(1000);
    // Thread 1: Transfer 500 from account1 to account2
    Thread thread1 = new Thread(() -> {
        account1.transfer(account2, 500);
    });
    // Thread 2: Transfer 700 from account1 to account2
    Thread thread2 = new Thread(() -> {
        account1.transfer(account2, 700);
    });

    // Start both threads
    thread1.start();
    thread2.start();

    // Wait for both threads to finish
    try {
        thread1.join();
        thread2.join();
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    assertTrue(account1.balance.get() >= 0);
}
```

If Thread 1 and Thread 2 run concurrently, they might both pass the check `if (this.balance >= amount)` before either of them deducts the balance. For example:
- Thread 1 checks the balance (1000), sees it’s sufficient, and proceeds to transfer 500.
- At the same time, Thread 2 checks the balance (1000), also sees it’s sufficient, and proceeds to transfer 700.
- Both threads then deduct from the balance, leading to an incorrect state where 1200 has been transferred out of an 
  account that initially had only 1000.


Unfortunately, traditional testing frameworks such as JUnit does not test the application under different thread 
interleavings. In order to find bugs in the application, developers usually implement *stress* tests, which run the application with a large number of threads and hope to find bugs. 

```java 
@Test 
public void myStressTest() {
    for (int i = 0; i < 1000; i++) {
        testBankAccount();
    }
}
```

This approach is not effective because it is hard to reproduce the bug and the test is non-deterministic. More 
importantly, in the most cases, the test will not trigger the bug!

Fray is designed to solve this problem. Simply add `@ConcurrencyTest` to `testBankAccount`, 
and Fray will automatically test 
the application with different interleavings **deterministically**. So instead of spawning run the test
1000 times and hope to find a bug, why not using Fray to test different interleaving thoroughly.

```java
@ConcurrencyTest
public void testBankAccount() {
  ...
}
```

And Fray can help you find the bug!

```
Test: [engine:fray]/[class:BankAccountTest]/[method:testBankAccount] failed: report can be found at: /target/fray/fray-report/7212529822708563775
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.014 s <<< FAILURE! -- in BankAccountTest
[ERROR] BankAccountTest.testBankAccount -- Time elapsed: 0.014 s <<< ERROR!
java.lang.reflect.InvocationTargetException
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at org.pastalab.fray.junit.FrayTestExecutor.executeTest$lambda$0(FrayTestExecutor.kt:38)
        at org.pastalab.fray.core.command.LambdaExecutor.execute(Executor.kt:62)
        at org.pastalab.fray.core.TestRunner.run(TestRunner.kt:44)
        at org.pastalab.fray.junit.FrayTestExecutor.executeTest(FrayTestExecutor.kt:55)
        at org.pastalab.fray.junit.FrayTestExecutor.execute(FrayTestExecutor.kt:25)
        at org.pastalab.fray.junit.FrayTestExecutor.executeContainer(FrayTestExecutor.kt:88)
        at org.pastalab.fray.junit.FrayTestExecutor.execute(FrayTestExecutor.kt:22)
        at org.pastalab.fray.junit.FrayTestExecutor.executeContainer(FrayTestExecutor.kt:88)
        at org.pastalab.fray.junit.FrayTestExecutor.execute(FrayTestExecutor.kt:19)
        at org.pastalab.fray.junit.FrayTestEngine.execute(FrayTestEngine.kt:50)
Caused by: org.opentest4j.AssertionFailedError: expected: <true> but was: <false>
        at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
        at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
        at org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
        at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
        at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:31)
        at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:183)
        at BankAccountTest.testBankAccount(BankAccountTest.java:63)
        ... 11 more
```

Debugging the test failure is easy! You can replay the schedule that causes the bug with your favorite debugger!

```java
@ConcurrencyTest(
        replay = "PATH_TO_THE_FRAY_REPORT_FOLDER/recording_0"
)
```


## Requirements

Fray is a concurrency testing framework that runs on Java 21, but can test Java programs written in any version up to Java 21

## HowTo

### Use Fray as a Gradle Plugin

The easist way to use Fray is through gradle plugin.

> [!WARNING]
> We are actively working on Fray so to get the latest version of Fray, you may use the [Github packages](https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages). 


```kotlin
plugins {
    id("org.pastalab.fray.gradle") version "0.1.10"
}
```

The gradle plugin will configure your project to use Fray. You can write Fray tests similar to JUnit tests. 

```java 
@ExtendWith(FrayTestExtension.class)
public class TestClass {
  ...
    @ConcurrencyTest
    public void test() throws Exception {
      ...
    }
}
```

### Use Fray as a Maven Plugin

You may also use Fray as a Maven plugin. 

```xml
<!--Add following to the plugins section-->
<plugin>
    <groupId>org.pastalab.fray.maven</groupId>
    <artifactId>fray-maven-plugin</artifactId>
    <version>0.1.2</version>
    <executions>
        <execution>
            <id>prepare-fray</id>
            <goals>
                <goal>prepare-fray</goal>
            </goals>
        </execution>
    </executions>
</plugin>

<!--Add following to the dependencies section-->
<dependency>
    <groupId>org.pastalab.fray</groupId>
    <artifactId>junit</artifactId>
    <version>0.1.1</version>
    <scope>test</scope>
</dependency>
```


### Use Fray as a command line tool

In order to use Fray as a command line tool. You need to build Fray first.


#### Build Fray

- Run the following commands to build Fray

```bash
./gradlew build 
export PATH=$PATH:$(pwd)/bin
```

#### Example

Consider the following application:

```java
import java.util.concurrent.atomic.AtomicInteger;

public class FrayExample extends Thread {
  static Object o = new Object();
  static AtomicInteger a = new AtomicInteger();
  static volatile int b;
  public void run() {
    int x = a.getAndIncrement();
    synchronized(o) { 
      if (x == 0) {
        try { o.wait(); } catch (InterruptedException ignore) { }
      } else {
        o.notify();
      }
    } 
    b = x;
  }
  public static void main(String[] args) throws Exception {
    a = new AtomicInteger();
    b = 0;
    FrayExample[] threads = {new FrayExample(), new FrayExample()};
    for (var thread : threads) thread.start();
    for (var thread : threads) thread.join();
  }
}
```

```bash
javac FrayExample.java -d out
```

You can run the program with regular Java command 
```
java -cp ./out/ FrayExample
```

You can just replace `java` with `fray` in your command line to run the program with Fray!

```bash
fray -cp ./out/ example.FrayExample
```

Fray will run the application with a random scheduler:

```
Fray Testing:
Iterations: XXX
Bugs Found: XXX
```

If an error is found, fray will also report:

```
Error found, you may find the error report in XXX
```


To replay a schedule, you may run the following command:

```bash
fray --replay PATH_TO_THE_RECORDING_FOLDER -cp ./out/ example.FrayExample 
```


### Customized Fray Configuration

<details>
<summary>Click Me</summary>

You may also choose to provide a configuration file for the application you want to test. The configuration file should be in the following format:

```json
{
  "executor": {
    "clazz": "com.example.Main",
    "method": "main",
    "args": ["arg1", "arg2"],
    "classpaths": ["path/to/your/application.jar"],
    "properties": {"PROPERTY1": "VALUE1", "PROPERTY2": "VALUE2"}
  },
  "ignore_unhandled_exceptions": false,
  "timed_op_as_yield": false,
  "interleave_memory_ops": false,
  "max_scheduled_step": -1
}
```

- `executor` defines the entrypoint and environment of the application you want to test.
  - `clazz`: the main class of the application.
  - `method`: the main method of the application.
  - `args`: the arguments to the main method.
  - `classpaths`: the classpaths of the application.
  - `properties`: the system properties of the application.
- `ignore_unhandled_exceptions`: whether to treat unhandled exceptions as failures.
- `timed_op_as_yield`: whether to treat timed operations as yields otherwise they will be treated as no timeout op.
- `interleave_memory_ops`: whether to interleave memory operations.
- `max_scheduled_step`: the maximum number of scheduled steps. And Fray will throw `LivenessException` if the number of scheduled steps exceeds this value. If the value is -1, then there is no limit.


You may use the following gradle task to run Fray:

```bash
./gradlew runFray -PconfigPath=path/to/your/application_config.json -PextraArgs="extra args passed to Fray"
```

Here are the available extra args:

```
Options:
  -o=<text>                Report output directory.
  -i, --iter=<int>         Number of iterations.
  -f, --full               If the report should save full schedule. Otherwise,
                           Fray only saves schedules points if there are more
                           than one runnable threads.
  -l, --logger=(json|csv)  Logger type.
  --scheduler=(replay|fifo|pos|random|pct)
                           Scheduling algorithm.
  --no-fray                Runnning in no-Fray mode.
  --explore                Running in explore mode and Fray will continue if a
                           failure is found.
  --no-exit-on-bug         Fray will not immediately exit when a failure is
                           found.
  --run-config=(cli|json)  Run configuration for the application.
  -h, --help               Show this message and exit
```

#### Output 

The output of Fray will be saved in the `output` directory. The output directory contains the following files:
 
- `output.txt`: the Fray of the testing.
- `schedule_{id}.json/csv`: the schedule you can replay.

#### Replay a buggy schedule

Once Fray finds a bug as indicated in `output.txt`. You may replay it by providing the corresponding schedule.

```bash
./gradlew runFray -PconfigPath=path/to/your/application_config.json -PextraArgs="--scheduler=replay --path=path/to/schedule.json"
```

#### Example

```bash
echo '{
  "executor": {
    "clazz": "example.FrayExample",
    "method": "main",
    "args": [],
    "classpaths": ["CURRENT_DIR/out/"],
    "properties": {}
  },
  "ignore_unhandled_exceptions": false,
  "timed_op_as_yield": false,
  "interleave_memory_ops": false,
  "max_scheduled_step": -1
}' | sed "s|CURRENT_DIR|$(pwd)|g" > out/config.json
./gradlew runFray -PconfigPath="out/config.json" -PextraArgs="--iter=1000 --logger=json --scheduler=random -o=/tmp/fray-example/"
```

To replay that schedule, you may run the following command:

```bash
./gradlew runFray -PconfigPath="out/config.json" -PextraArgs="--iter=1000 --logger=json --scheduler=replay 
--path=PATH_TO_THE_RECORDING_FOLDER"
```
</details>
