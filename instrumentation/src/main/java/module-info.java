module cmu.pasta.fray.instrumentation {
    exports cmu.pasta.fray.instrumentation;
    opens cmu.pasta.fray.instrumentation;
    requires cmu.pasta.fray.runtime;
    requires kotlin.reflect;
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires org.objectweb.asm.commons;
    requires java.instrument;
    requires org.objectweb.asm.util;
    requires java.logging;
}