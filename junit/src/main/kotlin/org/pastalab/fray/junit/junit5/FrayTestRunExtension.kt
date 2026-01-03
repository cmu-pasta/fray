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
import org.pastalab.fray.core.randomness.RecordedRandomProvider
import org.pastalab.fray.core.scheduler.ReplayScheduler
import org.pastalab.fray.core.scheduler.Scheduler
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
          val randomPath = "${path.absolutePath}/random.json"
          val recordingPath = "${path.absolutePath}/recording.json"
          val scheduler =
              if (frayTestAnnotation.scheduler.java == ReplayScheduler::class.java) {
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
          val scheduler = frayTestAnnotation.scheduler.java.getConstructor().newInstance()
          Pair(scheduler, ControlledRandomProvider())
        }

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
                false,
                false,
                -1,
            ),
            testDir.toString(),
            frayTestAnnotation.iterations,
            60,
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
            ignoreTimedBlock = frayTestAnnotation.ignoreTimedBlock,
            sleepAsYield = frayTestAnnotation.sleepAsYield,
        )
    val runner = TestRunner(config)
    runner.run()?.let { throw it }
  }

  fun getSystemClassPaths(): List<String> {
    val classpath = System.getProperty("java.class.path")
    return classpath.split(File.pathSeparator)
  }
}
