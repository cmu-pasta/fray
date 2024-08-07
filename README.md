# Fray: General-Purpose Concurrency Testing 

## Requirements

Please make sure you have Java 21 installed. To build the native plugin, you also need to have `g++` and `cmake` installed.

## Build Fray

- Run the following commands to build Fray

```bash
./gradlew build 
export PATH=$PATH:$(pwd)/bin
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