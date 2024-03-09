package cmu.pasta.sfuzz.instrumentation.memory

import java.lang.reflect.Field
import java.lang.reflect.Modifier

class VolatileManager {
    private val volatileFields = HashMap<String, Boolean>()

    fun isVolatile(name: String, fieldName: String): Boolean {
        val key = "$name:$fieldName"
        if (key in volatileFields) {
            return volatileFields[key]!!
        }
        try {
            var clazz: Class<*>? = Class.forName(name)
            var field: Field? = null
            while (clazz != null) {
                try {
                    field = clazz.getDeclaredField(fieldName)
                } catch (e: NoSuchFileException) {
                    clazz = clazz.superclass
                }
            }
            if (field != null) {
                volatileFields[key] = field.modifiers and Modifier.VOLATILE != 0
            } else {
                volatileFields[key] = false
            }
        } catch (e: ClassNotFoundException) {
            volatileFields[key] = false
        }
        return volatileFields[key]!!
    }
}