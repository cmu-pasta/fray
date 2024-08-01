package cmu.edu.pasta.fray.junit.annotations

import kotlin.reflect.KClass
import org.junit.platform.commons.annotation.Testable

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Testable
annotation class Analyze(val expected: KClass<*> = Any::class)
