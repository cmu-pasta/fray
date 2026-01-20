package org.pastalab.fray.junit.junit5.annotations

import kotlin.reflect.KClass
import org.junit.jupiter.api.TestTemplate
import org.pastalab.fray.core.command.NetworkDelegateType
import org.pastalab.fray.core.command.SystemTimeDelegateType
import org.pastalab.fray.core.scheduler.POSScheduler
import org.pastalab.fray.core.scheduler.Scheduler

@Target(
    AnnotationTarget.FUNCTION,
)
@Retention(
    AnnotationRetention.RUNTIME,
)
@TestTemplate
annotation class ConcurrencyTest(
    val iterations: Int = 1000,
    val scheduler: KClass<out Scheduler> = POSScheduler::class,
    val networkDelegateType: NetworkDelegateType = NetworkDelegateType.NONE,
    val systemTimeDelegateType: SystemTimeDelegateType = SystemTimeDelegateType.NONE,
    val sleepAsYield: Boolean = false,
    val ignoreTimedBlock: Boolean = true,
    val replay: String = "",
) {
  companion object {

    private const val CURRENT_REPETITION_PLACEHOLDER: String = "{currentRepetition}"

    private const val TOTAL_REPETITIONS_PLACEHOLDER: String = "{totalRepetitions}"

    const val SHORT_DISPLAY_NAME =
        "repetition $CURRENT_REPETITION_PLACEHOLDER of $TOTAL_REPETITIONS_PLACEHOLDER"
  }
}

@Target(
    AnnotationTarget.FUNCTION,
)
@Retention(
    AnnotationRetention.RUNTIME,
)
@TestTemplate
annotation class FrayTest(
    val iterations: Int = 1000,
    val scheduler: KClass<out Scheduler> = POSScheduler::class,
    val networkDelegateType: NetworkDelegateType = NetworkDelegateType.NONE,
    val systemTimeDelegateType: SystemTimeDelegateType = SystemTimeDelegateType.NONE,
    val sleepAsYield: Boolean = false,
    val ignoreTimedBlock: Boolean = true,
    val resetClassLoaderPerIteration: Boolean = true,
    val abortThreadExecutionAfterMainExit: Boolean = false,
    val replay: String = "",
) {
  companion object {

    private const val CURRENT_REPETITION_PLACEHOLDER: String = "{currentRepetition}"

    private const val TOTAL_REPETITIONS_PLACEHOLDER: String = "{totalRepetitions}"

    const val SHORT_DISPLAY_NAME =
        "repetition $CURRENT_REPETITION_PLACEHOLDER of $TOTAL_REPETITIONS_PLACEHOLDER"
  }
}
