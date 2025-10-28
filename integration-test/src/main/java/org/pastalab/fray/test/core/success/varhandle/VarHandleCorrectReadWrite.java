package org.pastalab.fray.test.core.success.varhandle;

import java.lang.invoke.MethodHandles;

public class VarHandleCorrectReadWrite {
    static class SimpleObject {
        public int intValue = 1;
        public short shortValue = 2;
        public byte byteValue = 3;
        public long longValue = 4;
        public float floatValue = 5;
        public double doubleValue = 6;
        public char charValue = 7;
        public boolean booleanValue = false;
        public int[] intArray = new int[10];
    }

    public static void main(String []args) throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        SimpleObject obj = new SimpleObject();
        var VH_INT = MethodHandles.lookup().findVarHandle(SimpleObject.class, "intValue", int.class);
        assert VH_INT.getVolatile(obj).equals(1);

        var VH_SHORT = MethodHandles.lookup().findVarHandle(SimpleObject.class, "shortValue", short.class);
        assert VH_SHORT.getVolatile(obj).equals((short)2);

        var VH_BYTE = MethodHandles.lookup().findVarHandle(SimpleObject.class, "byteValue", byte.class);
        assert VH_BYTE.getVolatile(obj).equals((byte)3);

        var VH_LONG = MethodHandles.lookup().findVarHandle(SimpleObject.class, "longValue", long.class);
        assert VH_LONG.getVolatile(obj).equals((long)4);

        var VH_FLOAT = MethodHandles.lookup().findVarHandle(SimpleObject.class, "floatValue", float.class);
        assert VH_FLOAT.getVolatile(obj).equals((float)5);

        var VH_DOUBLE = MethodHandles.lookup().findVarHandle(SimpleObject.class, "doubleValue", double.class);
        assert VH_DOUBLE.getVolatile(obj).equals((double)6);

        var VH_CHAR = MethodHandles.lookup().findVarHandle(SimpleObject.class, "charValue", char.class);
        assert VH_CHAR.getVolatile(obj).equals((char)7);

        var VH_BOOLEAN = MethodHandles.lookup().findVarHandle(SimpleObject.class, "booleanValue", boolean.class);
        assert VH_BOOLEAN.getVolatile(obj).equals(false);

        var VH_INT_ARRAY = MethodHandles.lookup().findVarHandle(SimpleObject.class, "intArray", int[].class);
        int[] arr = (int[]) VH_INT_ARRAY.getVolatile(obj);
        assert arr.length == 10;
    }
}
