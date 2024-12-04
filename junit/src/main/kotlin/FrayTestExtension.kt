package org.pastalab.fray.junit

import java.lang.reflect.Method
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
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
import org.pastalab.fray.core.scheduler.POSScheduler
import org.pastalab.fray.junit.annotations.ConcurrencyTest

class FrayTestExtension : TestTemplateInvocationContextProvider {
  val workDir = Paths.get(System.getProperty("fray.workDir", "build/fray/fray-report"))

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
    if (!workDir.toFile().exists()) {
      workDir.toFile().mkdirs()
    }
    val workDir = createTempDirectory(this.workDir).absolutePathString()
    val schedulerInfo = concurrencyTest.scheduler
    val config =
        Configuration(
            ExecutionInfo(
                LambdaExecutor {},
                false,
                true,
                false,
                -1,
            ),
            workDir,
            totalRepetition(concurrencyTest, testMethod),
            60,
            POSScheduler(),
            ControlledRandom(),
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
    val repetition = concurrencyTest.value
    Preconditions.condition(repetition > 0) {
      "Configuration error: @ConcurrencyTest on method [$method] must be declared with a positive 'value'."
    }
    return repetition
  }
}
