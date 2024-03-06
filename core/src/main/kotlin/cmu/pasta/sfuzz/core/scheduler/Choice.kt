package cmu.pasta.sfuzz.core.scheduler

import kotlinx.serialization.Serializable

@Serializable
data class Choice(val selected: Int, val threadId: Long, val enabled: Int)