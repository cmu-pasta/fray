# Fray: General-Purpose Concurrency Testing 

[![Build](https://github.com/cmu-pasta/fray/actions/workflows/main.yml/badge.svg)](https://github.com/cmu-pasta/fray/actions/workflows/main.yml)


Fray is a general-purpose concurrency testing tool for Java applications. It performs deterministic concurrency testing using various testing algorithms.
Fray is designed to be easy to use and can be integrated into existing testing frameworks.

## Getting Started

> [!TIP]
> Want to try Fray now? Clone the repository and run `./gradlew build`. Then replace `java` with `fray` in your command 
> line to run your application with Fray.


Consider you have a bank account application (you may find the complete example in 
https://github.com/cmu-pasta/fray-examples/blob/main/fray-maven-example/src/test/java/BankAccountTest.java):

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

In order to find bugs in the application, developers usually implement *hammer* tests, which run the application with a large number of threads and hope to find bugs. 

```java 
@Test 
public void MyHammerTest() {
    for (int i = 0; i < 1000; i++) {
        testBankAccount();
    }
}
```

However, this approach is not effective because it is hard to reproduce the bug and the test is non-deterministic. 

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

## Requirements

Fray currently requires Java 21. We are working on supporting more versions of Java.


## HowTo

### Use Fray as a Gradle Plugin

The easist way to use Fray is through gradle plugin.

```kotlin
plugins {
    id("org.pastalab.fray.gradle") version "0.1"
}

tasks.test {
  dependsOn("frayTest")
}
```

The gradle plugin will configure your project to use Fray. You can write Fray tests similar to JUnit tests. 

```java 
public class TestClass {
  ...
    @ConcurrencyTest(
    )
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
fray --replay /tmp/report/schedule_0.json -cp ./out/ example.FrayExample 
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
./gradlew runFray -PconfigPath="out/config.json" -PextraArgs="--iter=1000 --logger=json --scheduler=replay --path=/tmp/fray-example/schedule_XXX.json"
```
</details>
