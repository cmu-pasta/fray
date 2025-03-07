package org.pastalab.fray.core.syncurity

class AbortEvaluationException : RuntimeException {
  constructor() : super()

  constructor(message: String) : super(message)
}
