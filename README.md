# Fray: An Efficient General-Purpose Concurrency Testing Platform for the JVM

## Requirements

Please make sure you have Java 21 installed. To build the native plugin, you also need to have `g++` and `cmake` installed.

## Build Fray

- Run the following commands to build Fray

```bash
./gradlew build 
```

## Example

We provide an example application in the `integration-tests/src/main/java/example/FrayExample.java`. You may run the following command to build it:

```bash
mkdir out
javac integration-tests/src/main/java/example/FrayExample.java -d out
```

You can run the program with regular Java command 
```
java -ea -cp ./out/ example.FrayExample
```
In the most cases, the program will show an `AssertionError`.

## Run Fray

### Push-button Testing

The easiest way to run Fray is to replace `java` with `fray` in your command line. For example, if you want to run the following command:

```bash
./bin/fray -cp ./out/ example.FrayExample
```

Fray will run the application with a random scheduler. Dependening on the schedule you may either see a `DeadlockException`:

```
Error found: cmu.pasta.fray.runtime.DeadlockException
Thread: 0
Stacktrace:
        at java.base/java.lang.Thread.getStackTrace(Thread.java:2450)
        at cmu.pasta.fray.core.GlobalContext.reportError(GlobalContext.kt:84)
        at cmu.pasta.fray.core.GlobalContext.checkDeadlock(GlobalContext.kt:813)
        at cmu.pasta.fray.core.GlobalContext.objectWaitImpl(GlobalContext.kt:324)
        at cmu.pasta.fray.core.GlobalContext.objectWait(GlobalContext.kt:345)
        at cmu.pasta.fray.core.RuntimeDelegate.onObjectWait(RuntimeDelegate.kt:87)
        at java.base/cmu.pasta.fray.runtime.Runtime.onObjectWait(Runtime.java:75)
        at java.base/java.lang.Object.wait(Object.java)
        at java.base/java.lang.Thread.join(Thread.java:2078)
        at java.base/java.lang.Thread.join(Thread.java:2154)
        at example.FrayExample.main(FrayExample.java:24)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at cmu.pasta.fray.core.command.MethodExecutor.execute(Executor.kt:50)
        at cmu.pasta.fray.core.TestRunner.run(TestRunner.kt:44)
        at cmu.pasta.fray.core.MainKt.main(Main.kt:9)
Thread: 1
Stacktrace:
        at java.base/java.lang.Object.wait0(Native Method)
        at java.base/java.lang.Object.wait(Object.java:366)
        at java.base/java.lang.Object.wait(Object.java:339)
        at example.FrayExample.run(FrayExample.java:12)
```

Or an `AssertionError`:

```
Error found: java.lang.reflect.InvocationTargetException
java.lang.reflect.InvocationTargetException
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:118)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at cmu.pasta.fray.core.command.MethodExecutor.execute(Executor.kt:50)
        at cmu.pasta.fray.core.TestRunner.run(TestRunner.kt:44)
        at cmu.pasta.fray.core.MainKt.main(Main.kt:9)
Caused by: java.lang.AssertionError
        at example.FrayExample.main(FrayExample.java:25)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
        ... 4 more
```

And you may find the recorded schedule in the `/tmp/report/` directory.

To replay a schedule, you may run the following command:

```bash
./bin/fray -cp ./out/ example.FrayExample --replay /tmp/report/schedule_0.json
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