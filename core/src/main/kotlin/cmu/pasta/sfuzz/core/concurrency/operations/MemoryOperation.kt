package cmu.pasta.sfuzz.core.concurrency.operations

import kotlinx.serialization.Serializable

@Serializable
data class MemoryOperation(val t: Type): Operation {
    @Serializable
    enum class Type {
        ATOMIC,
        UNSAFE,
        FIELD,
    }
}