package cmu.pasta.fray.core.scheduler

import kotlinx.serialization.Serializable

@Serializable data class Choice(val selected: Int, val threadId: Int, val enabled: Int, val enabledIds: List<Int>)
