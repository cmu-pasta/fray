package org.pastalab.fray.junit.annotations

import kotlin.reflect.KClass
import org.junit.platform.commons.annotation.Testable
import org.pastalab.fray.core.scheduler.POSScheduler
import org.pastalab.fray.core.scheduler.Scheduler

/**
 * Annotates a concurrency test method that should be run by Fray.
 *
 * @param expectedException The expected exception class. If [Any], no expected exception.
 * @param expectedLog The expected log message. If empty, any log will be accepted.
 * @param scheduler The scheduler class. Default is [POSScheduler].
 * @param iteration The number of iterations. Default is 1.
 * @param replay If [replay] is provided, the test will be replayed with the same recording.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Testable
annotation class ConcurrencyTest(
    val expectedException: KClass<*> = Any::class,
    val scheduler: KClass<out Scheduler> = POSScheduler::class,
    val iteration: Int = 1,
    val replay: String = "",
)
