module org.anonlab.fray.instrumentation.jdk {
    requires jdk.jlink;
    requires java.instrument;
    requires kotlin.stdlib;
    requires org.anonlab.fray.instrumentation.base;
    requires org.anonlab.fray.runtime;
    requires org.objectweb.asm.commons;
    requires org.objectweb.asm.util;
    exports org.anonlab.fray.instrumentation.jdk.agent;
    exports org.anonlab.fray.instrumentation.jdk.jlink;
}