package org.pastalab.fray.core.observers

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleRecording(val scheduled: Int, val enabled: List<Int>, val operation: String)
