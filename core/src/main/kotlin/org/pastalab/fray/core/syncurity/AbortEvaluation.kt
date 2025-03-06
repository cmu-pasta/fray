package org.pastalab.fray.core.syncurity

class AbortEvaluation : RuntimeException {
  constructor() : super()

  constructor(message: String) : super(message)
}
