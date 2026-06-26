package org.pastalab.fray.junit.junit5

import java.io.File
import java.lang.reflect.Method
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import org.junit.platform.commons.support.AnnotationSupport
import org.pastalab.fray.core.TestRunner
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.command.ExecutionInfo
import org.pastalab.fray.core.command.MethodExecutor
import org.pastalab.fray.core.observers.ScheduleRecording
import org.pastalab.fray.core.randomness.ControlledRandomProvider
import org.pastalab.fray.core.randomness.Randomness
import org.pastalab.fray.core.scheduler.ReplayScheduler
import org.pastalab.fray.core.scheduler.SURWScheduler
import org.pastalab.fray.junit.Common.getPath
import org.pastalab.fray.junit.junit5.annotations.FrayTest

class FrayTestRunExtension : InvocationInterceptor {
  override fun interceptTestTemplateMethod(
      invocation: InvocationInterceptor.Invocation<Void>,
      invocationContext: ReflectiveInvocationContext<Method>,
      extensionContext: ExtensionContext,
  ) {
    val testMethod = invocationContext.executable
    val clazz = extensionContext.requiredTestClass
    val frayTestAnnotation =
        AnnotationSupport.findAnnotation(
                testMethod,
                FrayTest::class.java,
            )
            .get()

    launchFrayTest(clazz.name, testMethod.name, frayTestAnnotation)
    invocation.skip()
  }

  fun launchFrayTest(className: String, methodName: String, frayTestAnnotation: FrayTest) {
    val testDir = org.pastalab.fray.junit.Common.getTestDir(className, methodName)
    if (!testDir.toFile().exists()) {
      testDir.toFile().mkdirs()
    }
    val (scheduler, random) =
        if (frayTestAnnotation.replay.isNotEmpty()) {
          val path = getPath(frayTestAnnotation.replay)
          val recordedRandom =
              Json.decodeFromString<Randomness>(File("${path.absolutePath}/random.json").readText())
          val scheduler =
              if (frayTestAnnotation.scheduler.java == ReplayScheduler::class.java) {
                val recordingPath = "${path.absolutePath}/recording.json"
                val scheduleRecordings =
                    Json.decodeFromString<List<ScheduleRecording>>(File(recordingPath).readText())
                ReplayScheduler(scheduleRecordings, recordedRandom)
              } else {
                // Reconstruct the scheduler from the annotated class, fed the recorded randomness.
                frayTestAnnotation.scheduler.java
                    .getConstructor(Randomness::class.java)
                    .newInstance(recordedRandom)
              }
          Pair(scheduler, ControlledRandomProvider())
        } else {
          val scheduler = frayTestAnnotation.scheduler.java.getConstructor().newInstance()
          Pair(scheduler, ControlledRandomProvider())
        }

    val timelineCoverageType =
        if (
            frayTestAnnotation.collectTimelineCoverage ==
                org.pastalab.fray.core.observers.TimelineCoverageType.NONE
        )
            null
        else frayTestAnnotation.collectTimelineCoverage
    val resolveStackTraceHash = scheduler is SURWScheduler
    val config =
        Configuration(
            ExecutionInfo(
                MethodExecutor(
                    className,
                    methodName,
                    emptyList(),
                    getSystemClassPaths(),
                    emptyMap(),
                ),
                frayTestAnnotation.ignoreUncaughtExceptions,
                false,
                -1,
            ),
            testDir,
            frayTestAnnotation.iterations,
            600,
            scheduler,
            random,
            fullSchedule = false,
            exploreMode = false,
            noExitWhenBugFound = true,
            isReplay = frayTestAnnotation.replay.isNotEmpty(),
            noFray = false,
            dummyRun = false,
            networkDelegateType = frayTestAnnotation.networkDelegateType,
            systemTimeDelegateType = frayTestAnnotation.systemTimeDelegateType,
            virtualTimeDelta = frayTestAnnotation.virtualTimeDelta,
            ignoreTimedBlock = frayTestAnnotation.ignoreTimedBlock,
            sleepAsYield = frayTestAnnotation.sleepAsYield,
            resetClassLoader = frayTestAnnotation.resetClassLoaderPerIteration,
            redirectStdout = false,
            abortThreadExecutionAfterMainExit =
                frayTestAnnotation.abortThreadExecutionAfterMainExit,
            resolveRacingOperationStackTraceHash = resolveStackTraceHash,
            timelineCoverageType = timelineCoverageType,
        )
    val runner = TestRunner(config)
    runner.run()?.let { throw it }
  }

  fun getSystemClassPaths(): List<String> {
    val classpath = System.getProperty("java.class.path")
    return classpath.split(File.pathSeparator)
  }
}
