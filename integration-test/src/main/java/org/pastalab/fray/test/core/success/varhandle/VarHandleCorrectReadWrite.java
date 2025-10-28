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

    public static void main(String[] args) throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        SimpleObject obj = new SimpleObject();

        // int
        var VH_INT = MethodHandles.lookup().findVarHandle(SimpleObject.class, "intValue", int.class);
        VH_INT.setVolatile(obj, 42);
        assert ((Integer) VH_INT.getVolatile(obj)).intValue() == 42;

        // short
        var VH_SHORT = MethodHandles.lookup().findVarHandle(SimpleObject.class, "shortValue", short.class);
        VH_SHORT.setVolatile(obj, (short) 12);
        assert ((Short) VH_SHORT.getVolatile(obj)).shortValue() == (short) 12;

        // byte
        var VH_BYTE = MethodHandles.lookup().findVarHandle(SimpleObject.class, "byteValue", byte.class);
        VH_BYTE.setVolatile(obj, (byte) 7);
        assert ((Byte) VH_BYTE.getVolatile(obj)).byteValue() == (byte) 7;

        // long
        var VH_LONG = MethodHandles.lookup().findVarHandle(SimpleObject.class, "longValue", long.class);
        VH_LONG.setVolatile(obj, 100L);
        assert ((Long) VH_LONG.getVolatile(obj)).longValue() == 100L;

        // float
        var VH_FLOAT = MethodHandles.lookup().findVarHandle(SimpleObject.class, "floatValue", float.class);
        VH_FLOAT.setVolatile(obj, 3.14f);
        assert ((Float) VH_FLOAT.getVolatile(obj)).floatValue() == 3.14f;

        // double
        var VH_DOUBLE = MethodHandles.lookup().findVarHandle(SimpleObject.class, "doubleValue", double.class);
        VH_DOUBLE.setVolatile(obj, 2.718);
        assert ((Double) VH_DOUBLE.getVolatile(obj)).doubleValue() == 2.718;

        // char
        var VH_CHAR = MethodHandles.lookup().findVarHandle(SimpleObject.class, "charValue", char.class);
        VH_CHAR.setVolatile(obj, 'Z');
        assert ((Character) VH_CHAR.getVolatile(obj)).charValue() == 'Z';

        // boolean
        var VH_BOOLEAN = MethodHandles.lookup().findVarHandle(SimpleObject.class, "booleanValue", boolean.class);
        VH_BOOLEAN.setVolatile(obj, true);
        assert ((Boolean) VH_BOOLEAN.getVolatile(obj)).booleanValue();

        // int[]
        var VH_INT_ARRAY = MethodHandles.lookup().findVarHandle(SimpleObject.class, "intArray", int[].class);
        int[] newArr = new int[] {9, 8, 7};
        VH_INT_ARRAY.setVolatile(obj, newArr);
        int[] readArr = (int[]) VH_INT_ARRAY.getVolatile(obj);
        assert readArr == newArr;
        assert readArr.length == 3;
        assert readArr[0] == 9;
    }
}
