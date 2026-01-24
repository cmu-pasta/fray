package org.pastalab.fray.junit.junit5

import java.io.File
import java.lang.reflect.Method
import java.nio.file.Files
import java.util.stream.Stream
import kotlin.io.path.Path
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.AnnotationSupport.isAnnotated
import org.junit.platform.commons.util.Preconditions
import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.command.ExecutionInfo
import org.pastalab.fray.core.command.LambdaExecutor
import org.pastalab.fray.core.observers.ScheduleRecording
import org.pastalab.fray.core.randomness.ControlledRandomProvider
import org.pastalab.fray.core.randomness.RecordedRandomProvider
import org.pastalab.fray.core.scheduler.ReplayScheduler
import org.pastalab.fray.core.scheduler.Scheduler
import org.pastalab.fray.junit.Common.getPath
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest
import org.pastalab.fray.junit.junit5.annotations.FrayTest

class FrayTestExtension : TestTemplateInvocationContextProvider {

  val enabled: Boolean by lazy {
    val release = Path(System.getProperty("java.home"), "release")
    for (line in Files.readAllLines(release)) {
      if (line.contains("IMPLEMENTOR=Fray")) {
        return@lazy true
      }
    }
    false
  }

  override fun supportsTestTemplate(context: ExtensionContext): Boolean {
    return isAnnotated(context.testMethod, ConcurrencyTest::class.java) ||
        isAnnotated(context.testMethod, FrayTest::class.java)
  }

  override fun provideTestTemplateInvocationContexts(
      context: ExtensionContext
  ): Stream<TestTemplateInvocationContext> {
    val testMethod = context.requiredTestMethod
    val displayName = context.displayName
    val testClass = context.requiredTestClass

    if (!enabled) {
      return Stream.of(alwaysDisabledInvocation(displayName, "Fray is not enabled in this JVM"))
    }

    if (isAnnotated(testMethod, FrayTest::class.java)) {
      return Stream.of(FrayTestInvocationContext())
    }

    val concurrencyTest: ConcurrencyTest =
        AnnotationSupport.findAnnotation(
                testMethod,
                ConcurrencyTest::class.java,
            )
            .get()

    val repetitions = totalRepetition(concurrencyTest, testMethod)

    // Use the test class and method names for the report directory
    val className = testClass.name
    val methodName = testMethod.name
    val testDir = org.pastalab.fray.junit.Common.getTestDir(className, methodName)

    if (!testDir.toFile().exists()) {
      testDir.toFile().mkdirs()
    }

    val (scheduler, random) =
        if (concurrencyTest.replay.isNotEmpty()) {
          val path = getPath(concurrencyTest.replay)
          val randomPath = "${path.absolutePath}/random.json"
          val recordingPath = "${path.absolutePath}/recording.json"
          val scheduler =
              if (concurrencyTest.scheduler.java == ReplayScheduler::class.java) {
                val scheduleRecordings =
                    Json.decodeFromString<List<ScheduleRecording>>(File(recordingPath).readText())
                ReplayScheduler(scheduleRecordings)
              } else {
                val schedulerPath = "${path.absolutePath}/schedule.json"
                Json.decodeFromString<Scheduler>(File(schedulerPath).readText())
              }
          val randomnessProvider = RecordedRandomProvider(randomPath)
          Pair(scheduler, randomnessProvider)
        } else {
          val scheduler = concurrencyTest.scheduler.java.getConstructor().newInstance()
          Pair(scheduler, ControlledRandomProvider())
        }
    val config =
        Configuration(
            ExecutionInfo(
                LambdaExecutor {},
                ignoreUnhandledExceptions = false,
                interleaveMemoryOps = false,
                maxScheduledStep = -1,
            ),
            testDir,
            repetitions,
            60,
            scheduler,
            random,
            fullSchedule = false,
            exploreMode = false,
            noExitWhenBugFound = true,
            isReplay = concurrencyTest.replay.isNotEmpty(),
            noFray = false,
            dummyRun = false,
            networkDelegateType = concurrencyTest.networkDelegateType,
            systemTimeDelegateType = concurrencyTest.systemTimeDelegateType,
            ignoreTimedBlock = concurrencyTest.ignoreTimedBlock,
            sleepAsYield = concurrencyTest.sleepAsYield,
            resetClassLoader = false,
            redirectStdout = false,
            abortThreadExecutionAfterMainExit = false,
        )
    val frayContext = RunContext(config)
    val frayJupiterContext = FrayJupiterContext()
    return Stream.iterate(1, { it + 1 }).sequential().limit(repetitions.toLong()).map {
      ConcurrencyTestInvocationContext(it, displayName, frayContext, frayJupiterContext)
    }
  }

  private fun totalRepetition(concurrencyTest: ConcurrencyTest, method: Method): Int {
    // If the user guide the program execution through IDE plugin, the repetition is set to 1
    val repetition =
        if (
            System.getProperty("fray.debugger", "false").toBoolean() ||
                concurrencyTest.replay.isNotEmpty()
        ) {
          1
        } else {
          concurrencyTest.iterations
        }
    Preconditions.condition(repetition > 0) {
      "Configuration error: @ConcurrencyTest on method [$method] must be declared with a positive 'value'."
    }
    return repetition
  }

  private fun alwaysDisabledInvocation(
      displayName: String,
      reason: String,
  ): TestTemplateInvocationContext {
    return object : TestTemplateInvocationContext {
      override fun getDisplayName(invocationIndex: Int): String {
        return "$displayName (skipped)"
      }

      override fun getAdditionalExtensions(): MutableList<Extension> {
        return mutableListOf(
            object : ExecutionCondition {
              override fun evaluateExecutionCondition(
                  context: ExtensionContext
              ): ConditionEvaluationResult {
                return ConditionEvaluationResult.disabled(reason)
              }
            }
        )
      }
    }
  }
}
