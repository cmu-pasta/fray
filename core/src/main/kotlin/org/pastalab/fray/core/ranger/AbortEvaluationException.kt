package org.pastalab.fray.core.ranger

class AbortEvaluationException : RuntimeException {
  constructor() : super()

  constructor(message: String) : super(message)
}
