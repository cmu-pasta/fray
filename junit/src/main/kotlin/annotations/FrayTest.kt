package org.pastalab.fray.junit.annotations

import org.junit.platform.commons.annotation.Testable

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Testable
annotation class FrayTest()
