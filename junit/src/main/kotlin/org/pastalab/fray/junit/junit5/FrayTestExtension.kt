package org.pastalab.fray.junit.junit5

import java.io.File
import java.lang.reflect.Method
import java.util.stream.Stream
import kotlinx.serialization.json.Json
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
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.scheduler.ReplayScheduler
import org.pastalab.fray.core.scheduler.Scheduler
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest

class FrayTestExtension : TestTemplateInvocationContextProvider {

  override fun supportsTestTemplate(context: ExtensionContext): Boolean {
    return isAnnotated(context.testMethod, ConcurrencyTest::class.java)
  }

  override fun provideTestTemplateInvocationContexts(
      context: ExtensionContext
  ): Stream<TestTemplateInvocationContext> {
    val testMethod = context.requiredTestMethod
    val displayName = context.displayName
    val testClass = context.requiredTestClass
    val concurrencyTest: ConcurrencyTest =
        AnnotationSupport.findAnnotation(
                testMethod,
                ConcurrencyTest::class.java,
            )
            .get()

    // Use the test class and method names for the report directory
    val className = testClass.name
    val methodName = testMethod.name
    val testDir = org.pastalab.fray.junit.Common.getTestDir(className, methodName)

    if (!testDir.toFile().exists()) {
      testDir.toFile().mkdirs()
    }

    // Check for command-line overrides of the scheduler, iterations, etc.
    val customSchedulerName = System.getProperty("fray.scheduler")
    val customIterations = System.getProperty("fray.iterations")?.toIntOrNull()
    val customNumSwitchPoints = System.getProperty("fray.numSwitchPoints")?.toIntOrNull()
    val customReplayDir = System.getProperty("fray.replayDir")
    val customTimeout = System.getProperty("fray.timeout")?.toIntOrNull()
    val exploreMode = System.getProperty("fray.exploreMode")?.toBoolean() ?: false

    val (scheduler, random) =
        if (customReplayDir != null || concurrencyTest.replay.isNotEmpty()) {
          val replayPath = customReplayDir ?: concurrencyTest.replay
          val path = getPath(replayPath)
          val randomPath = "${path.absolutePath}/random.json"
          val recordingPath = "${path.absolutePath}/recording.json"
          val scheduler =
              if ((customSchedulerName == null &&
                  concurrencyTest.scheduler.java == ReplayScheduler::class.java) ||
                  customSchedulerName == "replay") {
                val scheduleRecordings =
                    Json.decodeFromString<List<ScheduleRecording>>(File(recordingPath).readText())
                ReplayScheduler(scheduleRecordings)
              } else {
                val schedulerPath = "${path.absolutePath}/schedule.json"
                Json.decodeFromString<Scheduler>(File(schedulerPath).readText())
              }
          val randomnessProvider =
              Json.decodeFromString<ControlledRandom>(File(randomPath).readText())
          Pair(scheduler, randomnessProvider)
        } else {
          // Use custom scheduler if provided
          val schedulerInstance =
              when (customSchedulerName) {
                "pct" ->
                    Class.forName("org.pastalab.fray.core.scheduler.PCTScheduler")
                        .getConstructor(Int::class.java)
                        .newInstance(customNumSwitchPoints ?: 3)
                "random" ->
                    Class.forName("org.pastalab.fray.core.scheduler.RandomScheduler")
                        .getConstructor()
                        .newInstance()
                "surw" ->
                    Class.forName("org.pastalab.fray.core.scheduler.SURWScheduler")
                        .getConstructor()
                        .newInstance()
                "pos" ->
                    Class.forName("org.pastalab.fray.core.scheduler.POSScheduler")
                        .getConstructor()
                        .newInstance()
                else ->
                    Class.forName("org.pastalab.fray.core.scheduler.RandomScheduler")
                        .getConstructor()
                        .newInstance()
              }

          Pair(schedulerInstance as Scheduler, ControlledRandom())
        }

    // Use the custom iterations if provided, otherwise use the value from the annotation
    val iterations = customIterations ?: totalRepetition(concurrencyTest, testMethod)

    // Use the custom timeout if provided, otherwise use the default value
    val timeout = customTimeout ?: 60

    val config =
        Configuration(
            ExecutionInfo(
                LambdaExecutor {},
                false,
                false,
                -1,
            ),
            testDir.toString(),
            iterations,
            timeout,
            scheduler,
            random,
            false,
            exploreMode,
            true,
            (customReplayDir != null || concurrencyTest.replay.isNotEmpty()),
            false,
            true,
        )
    config.frayLogger.info("System property for fray.scheduler: $customSchedulerName")
    config.frayLogger.info("System property for fray.iterations: $customIterations")
    config.frayLogger.info("System property for fray.numSwitchPoints: $customNumSwitchPoints")
    config.frayLogger.info("System property for fray.replayDir: $customReplayDir")
    config.frayLogger.info("System property for fray.timeout: $customTimeout")
    config.frayLogger.info("System property for fray.exploreMode: $exploreMode")
    val frayContext = RunContext(config)
    val frayJupiterContext = FrayJupiterContext()
    return Stream.iterate(1, { it + 1 })
        .sequential()
        .limit(totalRepetition(concurrencyTest, testMethod).toLong())
        .map { FrayTestInvocationContext(it, displayName, frayContext, frayJupiterContext) }
  }

  private fun totalRepetition(concurrencyTest: ConcurrencyTest, method: Method): Int {
    // If the user guide the program execution through IDE plugin, the repetition is set to 1
    val repetition =
        if (System.getProperty("fray.debugger", "false").toBoolean() ||
            concurrencyTest.replay.isNotEmpty()) {
          1
        } else {
          concurrencyTest.iterations
        }
    Preconditions.condition(repetition > 0) {
      "Configuration error: @ConcurrencyTest on method [$method] must be declared with a positive 'value'."
    }
    return repetition
  }

  fun getPath(resourceLocation: String): File {
    val classPathPrefix = "classpath:"
    return if (resourceLocation.startsWith(classPathPrefix)) {
      val classPathPath = resourceLocation.substring(classPathPrefix.length)
      val classLoader = Thread.currentThread().getContextClassLoader()
      val url = classLoader.getResource(classPathPath)
      File(url.toURI())
    } else {
      File(resourceLocation)
    }
  }
}
