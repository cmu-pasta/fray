# Fray: General-Purpose Concurrency Testing 

[![Build](https://github.com/cmu-pasta/fray/actions/workflows/main.yml/badge.svg)](https://github.com/cmu-pasta/fray/actions/workflows/main.yml)


Fray is a general-purpose concurrency testing tool for Java applications. It performs deterministic concurrency testing using various testing algorithms.
Fray is designed to be easy to use and can be integrated into existing testing frameworks.

# Quick Start

## Gradle

To use Fray with Gradle, add the following plugin to your `build.gradle` file:

```kotlin
plugins {
    id("org.pastalab.fray.gradle") version "0.2.2"
}
```

## Maven

- First please add Fray plugin to your project

```
<plugin>
    <groupId>org.pastalab.fray.maven</groupId>
    <artifactId>fray-plugins-maven</artifactId>
    <version>0.2.2</version>
    <executions>
        <execution>
            <id>prepare-fray</id>
            <goals>
                <goal>prepare-fray</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

- Next, please add the `fray-junit` dependency

```
<dependency>
    <groupId>org.pastalab.fray</groupId>
    <artifactId>fray-junit</artifactId>
    <version>0.2.2</version>
    <scope>test</scope>
</dependency>
```

## JUnit 5

If you are using JUnit 5, you can use the `@ConcurrencyTest` annotation to mark a test as a concurrency test. You 
also need to add the `@ExtendWith(FrayTestExtension.class)` annotation to the test class.

```java
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

@ExtendWith(FrayTestExtension.class)
public class SimpleTest {
    @ConcurrencyTest
    public void concurrencyTest() {
        ...
    }
}

```


## Other Testing Frameworks

Fray can be used with other testing frameworks as well. You may use the `FrayInTestLauncher`

```java
import org.pastalab.fray.junit.plain.FrayInTestLauncher;
FrayInTestLauncher.INSTANCE.launchFrayTest(() -> {
    ...
});
```