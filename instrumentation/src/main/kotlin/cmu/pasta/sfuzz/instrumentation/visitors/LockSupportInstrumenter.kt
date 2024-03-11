package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import java.util.concurrent.locks.LockSupport

class LockSupportInstrumenter(cv: ClassVisitor): ClassVisitorBase(cv, LockSupport::class.java.name) {
    
}