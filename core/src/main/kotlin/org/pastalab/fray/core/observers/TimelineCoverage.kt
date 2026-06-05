package org.pastalab.fray.core.observers

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.ScheduleObserver
import org.pastalab.fray.rmi.TestStatusObserver

interface TimelineCoverage : ScheduleObserver<ThreadContext>, TestStatusObserver {
  fun getCoverage(): Int
}

enum class TimelineCoverageType {
  THREAD_ORDERING,
  RESOURCE_ORDERING,
  NONE,
}
