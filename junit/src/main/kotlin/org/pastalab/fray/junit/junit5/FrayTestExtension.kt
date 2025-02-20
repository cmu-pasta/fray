package org.pastalab.fray.junit.junit5

import java.io.File
import java.lang.reflect.Method
import java.util.stream.Stream
import kotlin.io.path.absolutePathString
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
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.scheduler.Scheduler
import org.pastalab.fray.junit.Common.WORK_DIR
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
    val concurrencyTest: ConcurrencyTest =
        AnnotationSupport.findAnnotation(
                testMethod,
                ConcurrencyTest::class.java,
            )
            .get()
    if (!WORK_DIR.toFile().exists()) {
      WORK_DIR.toFile().mkdirs()
    }
    val (scheduler, random) =
        if (concurrencyTest.replay.isNotEmpty()) {
          val path = concurrencyTest.replay
          val randomPath = "${path}/random.json"
          val schedulerPath = "${path}/schedule.json"
          val randomnessProvider =
              Json.decodeFromString<ControlledRandom>(File(randomPath).readText())
          val scheduler = Json.decodeFromString<Scheduler>(File(schedulerPath).readText())
          Pair(scheduler, randomnessProvider)
        } else {
          val scheduler = concurrencyTest.scheduler.java.getConstructor().newInstance()
          Pair(scheduler, ControlledRandom())
        }
    val config =
        Configuration(
            ExecutionInfo(
                LambdaExecutor {},
                false,
                false,
                -1,
            ),
            WORK_DIR.absolutePathString(),
            totalRepetition(concurrencyTest, testMethod),
            60,
            scheduler,
            random,
            false,
            false,
            true,
            false,
            false,
            true,
        )
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
        if (System.getProperty("fray.debugger", "false").toBoolean()) {
          1
        } else {
          concurrencyTest.iterations
        }
    Preconditions.condition(repetition > 0) {
      "Configuration error: @ConcurrencyTest on method [$method] must be declared with a positive 'value'."
    }
    return repetition
  }
}
