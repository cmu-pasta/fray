package cmu.pasta.fray.runner;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.Result;

public class JUnitRunner {
    public static void main(String[] args) throws ClassNotFoundException {
        boolean isJunit4 = args[0].equals("junit4");
        String[] classAndMethod = args[1].split("#");

        if (isJunit4) {
            Request request = Request.method(
                    Class.forName(classAndMethod[0], true, Thread.currentThread().getContextClassLoader()),
                    classAndMethod[1]
            ).filterWith(new Filter() {
                private boolean found = false;

                @Override
                public boolean shouldRun(Description description) {
                    if (found) return false;
                    if (description.getMethodName().equals(classAndMethod[1])) {
                        found = true;
                    }
                    return found;
                }

                @Override
                public String describe() {
                    return classAndMethod[1];
                }
            });

            Result result = new JUnitCore().run(request);
            if (!result.wasSuccessful()) {
                StringBuilder failureReport = new StringBuilder();
                for (Failure failure : result.getFailures()) {
                    failureReport.append("testHeader: ").append(failure.getTestHeader()).append("\n")
                            .append("trace: ").append(failure.getTrace()).append("\n")
                            .append("description: ").append(failure.getDescription()).append("\n");
                }
                System.out.println(failureReport.toString());
                throw new RuntimeException(failureReport.toString());
            }
        } else {
            Class[] parameterTypes = new Class[0];

            String testClassName = classAndMethod[0];
            Class<?> testClass = Class.forName(testClassName, true, Thread.currentThread().getContextClassLoader());
            for (Method method : testClass.getDeclaredMethods()) {
                if (method.getName().equals(classAndMethod[1])) {
                    System.out.println(method.getName());
                    parameterTypes = method.getParameterTypes();
                }
            }
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectMethod(classAndMethod[0], classAndMethod[1], parameterTypes))
                    .build();
            Launcher launcher = LauncherFactory.create();
            SummaryGeneratingListener listener = new SummaryGeneratingListener();
            launcher.registerTestExecutionListeners(listener);
            try {
                launcher.execute(request);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (listener.getSummary().getTestsFailedCount() > 0) {
                StringBuilder failureReport = new StringBuilder();
                listener.getSummary().getFailures().forEach(failure -> {
                    StringWriter stringWriter = new StringWriter();
                    PrintWriter writer = new PrintWriter(stringWriter);
                    failure.getException().printStackTrace(writer);
                    failureReport.append("testHeader: ").append(failure.getTestIdentifier()).append("\n")
                            .append("trace: ").append(stringWriter.toString()).append("\n")
                            .append("exception: ").append(failure.getException()).append("\n");
                });
                System.out.println(failureReport.toString());
                throw new RuntimeException(failureReport.toString());
            }
        }
    }
}