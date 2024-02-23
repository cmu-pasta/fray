package cmu.pasta.sfuzz.jdk.visitors

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

fun exampleMethod(param1: String, param2: Int): Long = 42L
class UtilsTest {

    @Test
    fun kFunctionToJvmMethodDescriptor() {
        assertEquals(Utils.kFunctionToJvmMethodDescriptor(::exampleMethod), "(Ljava/lang/String;I)J")
    }
}