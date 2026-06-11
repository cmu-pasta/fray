using Fray.Core;
using Fray.Core.Contexts;

namespace Fray;

/// <summary>
/// A shared variable whose reads and writes are scheduling points, letting
/// Fray explore data-race interleavings. This is the wrapper-based equivalent
/// of the memory-access instrumentation of the JVM implementation.
/// </summary>
public sealed class FrayShared<T>
{
    private T _value;

    public FrayShared(T initialValue) => _value = initialValue;

    public T Value
    {
        get
        {
            Hook(MemoryOpType.MemoryRead);
            return _value;
        }
        set
        {
            Hook(MemoryOpType.MemoryWrite);
            _value = value;
        }
    }

    public T Read() => Value;

    public void Write(T value) => Value = value;

    private void Hook(MemoryOpType type) =>
        FrayRuntime.ControlledContext()?.MemoryOperation(ObjectIds.Of(this), type);
}

/// <summary>
/// Controlled atomic integer, equivalent to Java's <c>AtomicInteger</c>.
/// Each operation is a single scheduling point; the operation itself is
/// performed atomically (via <see cref="Interlocked"/> when uncontrolled).
/// </summary>
public sealed class FrayAtomicInt32
{
    private int _value;

    public FrayAtomicInt32(int initialValue = 0) => _value = initialValue;

    public int Value
    {
        get
        {
            Hook(MemoryOpType.MemoryRead);
            return Volatile.Read(ref _value);
        }
        set
        {
            Hook(MemoryOpType.MemoryWrite);
            Volatile.Write(ref _value, value);
        }
    }

    public int IncrementAndGet() => AddAndGet(1);

    public int DecrementAndGet() => AddAndGet(-1);

    public int AddAndGet(int delta)
    {
        Hook(MemoryOpType.MemoryWrite);
        return Interlocked.Add(ref _value, delta);
    }

    public bool CompareAndSet(int expected, int newValue)
    {
        Hook(MemoryOpType.MemoryWrite);
        return Interlocked.CompareExchange(ref _value, newValue, expected) == expected;
    }

    private void Hook(MemoryOpType type) =>
        FrayRuntime.ControlledContext()?.MemoryOperation(ObjectIds.Of(this), type);
}
