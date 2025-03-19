module org.anonlab.fray.instrumentation.base {
    exports org.anonlab.fray.instrumentation.base;
    exports org.anonlab.fray.instrumentation.base.visitors;
    opens org.anonlab.fray.instrumentation.base;
    requires org.anonlab.fray.runtime;
    requires kotlin.reflect;
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires org.objectweb.asm.commons;
    requires java.instrument;
    requires org.objectweb.asm.util;
    requires java.logging;
}