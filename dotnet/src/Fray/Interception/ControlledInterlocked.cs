using Fray.Core;

namespace Fray.Interception;

/// <summary>
/// Replacements for <see cref="Interlocked"/> used by the IL rewriter: each
/// operation is a single scheduling point followed by the real atomic op.
/// The address of a <c>ref</c> argument cannot be hashed, so all interlocked
/// operations conservatively share one race resource.
/// </summary>
public static class ControlledInterlocked
{
    private const int InterlockedResource = 0;

    private static void Hook() =>
        FrayRuntime.ControlledContext()?.MemoryOperation(InterlockedResource, MemoryOpType.MemoryWrite);

    public static int Increment(ref int location)
    {
        Hook();
        return Interlocked.Increment(ref location);
    }

    public static long Increment(ref long location)
    {
        Hook();
        return Interlocked.Increment(ref location);
    }

    public static int Decrement(ref int location)
    {
        Hook();
        return Interlocked.Decrement(ref location);
    }

    public static long Decrement(ref long location)
    {
        Hook();
        return Interlocked.Decrement(ref location);
    }

    public static int Add(ref int location, int value)
    {
        Hook();
        return Interlocked.Add(ref location, value);
    }

    public static long Add(ref long location, long value)
    {
        Hook();
        return Interlocked.Add(ref location, value);
    }

    public static int Exchange(ref int location, int value)
    {
        Hook();
        return Interlocked.Exchange(ref location, value);
    }

    public static long Exchange(ref long location, long value)
    {
        Hook();
        return Interlocked.Exchange(ref location, value);
    }

    public static object? Exchange(ref object? location, object? value)
    {
        Hook();
        return Interlocked.Exchange(ref location, value);
    }

    public static int CompareExchange(ref int location, int value, int comparand)
    {
        Hook();
        return Interlocked.CompareExchange(ref location, value, comparand);
    }

    public static long CompareExchange(ref long location, long value, long comparand)
    {
        Hook();
        return Interlocked.CompareExchange(ref location, value, comparand);
    }

    public static object? CompareExchange(ref object? location, object? value, object? comparand)
    {
        Hook();
        return Interlocked.CompareExchange(ref location, value, comparand);
    }
}
