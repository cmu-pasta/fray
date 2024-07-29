package cmu.edu.pasta.fray.junit.annotations

import org.junit.platform.commons.annotation.Testable

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Testable
annotation class Analyze()
