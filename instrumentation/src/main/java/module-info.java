module org.pastalab.fray.instrumentation {
    exports org.pastalab.fray.instrumentation;
    opens org.pastalab.fray.instrumentation;
    requires org.pastalab.fray.runtime;
    requires kotlin.reflect;
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires org.objectweb.asm.commons;
    requires java.instrument;
    requires org.objectweb.asm.util;
    requires java.logging;
}