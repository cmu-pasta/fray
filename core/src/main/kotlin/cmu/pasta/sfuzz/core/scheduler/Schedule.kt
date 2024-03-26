package cmu.pasta.sfuzz.core.scheduler

import kotlinx.serialization.Serializable

@Serializable data class Schedule(val choices: MutableList<Choice>, val fullSchedule: Boolean)
