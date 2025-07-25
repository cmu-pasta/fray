package org.pastalab.fray.test.core.success.classconstructor;

import java.io.IOException;
import java.io.InputStream;

public class YieldInClassConstructor {
    protected static class TestClass {
        static boolean staticField = false;
        static {
            Thread.yield();
        }

        public TestClass() {
        }
    }
    protected static class DynamicClassLoader extends ClassLoader {
        public DynamicClassLoader() {
            super(null);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.equals("org.pastalab.fray.test.core.success.classconstructor.ClassConstructorNoDeadlock$TestClass")) {
                return findClass(name);
            }

            return super.loadClass(name);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String resource = name.replace('.', '/').concat(".class");
            try (InputStream is = ClassConstructorNoDeadlock.class.getClassLoader().getResourceAsStream(resource)) {
                if (is == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = is.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        DynamicClassLoader dynamicClassLoader = new DynamicClassLoader();

        Class<?> clazz = dynamicClassLoader.loadClass("org.pastalab.fray.test.core.success.classconstructor.YieldInClassConstructor$TestClass");
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    TestClass.staticField = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        Object instance = clazz.getDeclaredConstructor().newInstance();
    }
}

