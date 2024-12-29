# Fray Gradle Plugin

This plugin helps you to use Fray in your Gradle project.

```kotlin   
plugins {
    id("cmu.pasta.fray.gradle") version "0.1"
}
```

Now you can write unit tests and run them with Fray.

```java

@FrayTest
class MyTest {
    @Analyze 
    public void test() {
        // Your test code here
    }
}
```

To run all Fray tests, use the following command:

```shell
./gradlew frayTest
```


## (Optional) Configuration

```kotlin
fray {
    // You may specifiy the version of fray you would like to use. 
    version = "0.1"
}

tasks.test {
  // You may also make the test task depend on the frayTest task
  // `./gradlew test` will run the fray tests 
  dependsOn("frayTest")
}
```