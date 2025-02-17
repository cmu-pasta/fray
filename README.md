# Fray: General-Purpose Concurrency Testing 

[![Build](https://github.com/cmu-pasta/fray/actions/workflows/main.yml/badge.svg)](https://github.com/cmu-pasta/fray/actions/workflows/main.yml)


Fray is a concurrency testing tool for Java that can help you **find and debug tricky race conditions that manifest as assertion violations, run-time exceptions, or deadlocks**.
It performs controlled concurrency testing using state-of-the-art techniques such as [probabilistic concurrency testing](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/asplos277-pct.pdf) 
or [partial order sampling](https://www.cs.columbia.edu/~junfeng/papers/pos-cav18.pdf). Fray also provides deterministic replay capabilities for debugging specific thread interleavings. Fray is designed 
to be easy to use and can be integrated into existing testing frameworks.

# Quick Start

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
        ... // some multithreaded code
        assert(...);
    }
}

```


## Other Testing Frameworks

Fray can be used with other testing frameworks as well. You may use the `FrayInTestLauncher`

```java
import org.pastalab.fray.junit.plain.FrayInTestLauncher;

public void test() {
    FrayInTestLauncher.INSTANCE.launchFrayTest(() -> {
        ... // some multithreaded code
        assert(...);
    });
}
```

## Gradle

To use Fray with Gradle, add the following plugin to your `build.gradle` file:

```kotlin
plugins {
    id("org.pastalab.fray.gradle") version "0.2.5"
}
```

## Maven

- First please add Fray plugin to your project

```xml
<plugin>
    <groupId>org.pastalab.fray.maven</groupId>
    <artifactId>fray-plugins-maven</artifactId>
    <version>0.2.5</version>
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

```xml
<dependency>
    <groupId>org.pastalab.fray</groupId>
    <artifactId>fray-junit</artifactId>
    <version>0.2.5</version>
    <scope>test</scope>
</dependency>
```


# Documentation

- [Technical Report](https://arxiv.org/abs/2501.12618)
- [Usage Guide](./docs/usage.md)
- [IDE Settings](./docs/IDE.md)
- [Bugs Found by Fray and Testimonies](./docs/bugs.md)

# Acknowledgements

This project was funded in part by an Amazon Research Award and NSF grant CCF-2120955.