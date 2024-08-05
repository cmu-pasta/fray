module org.pastalab.fray.jdk {
    requires jdk.jlink;
    requires java.instrument;
    requires kotlin.stdlib;
    requires org.pastalab.fray.instrumentation;
    requires org.pastalab.fray.runtime;
    exports org.pastalab.fray.jdk.agent;
    exports org.pastalab.fray.jdk.jlink;
}