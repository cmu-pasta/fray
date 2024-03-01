module cmu.pasta.sfuzz.instrumentation {
    exports cmu.pasta.sfuzz.instrumentation;
    opens cmu.pasta.sfuzz.instrumentation;
    requires cmu.pasta.sfuzz.runtime;
    requires kotlin.reflect;
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires org.objectweb.asm.commons;
    requires java.instrument;
}