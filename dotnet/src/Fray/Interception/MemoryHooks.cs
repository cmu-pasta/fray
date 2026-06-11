using Fray.Core;
using Fray.Core.Contexts;

namespace Fray.Interception;

/// <summary>
/// Scheduling-point hooks inserted by the IL rewriter before field accesses,
/// the wrapper-free equivalent of <see cref="FrayShared{T}"/>. The resource
/// identity combines the target object with a stable hash of the field name,
/// mirroring the JVM implementation's <c>fieldOperation</c>.
/// </summary>
public static class MemoryHooks
{
    public static void OnFieldRead(object? target, int fieldId)
    {
        if (target == null)
        {
            return; // Let the actual access throw NullReferenceException.
        }
        FrayRuntime.ControlledContext()?.MemoryOperation(Combine(ObjectIds.Of(target), fieldId), MemoryOpType.MemoryRead);
    }

    public static void OnFieldWrite(object? target, int fieldId)
    {
        if (target == null)
        {
            return;
        }
        FrayRuntime.ControlledContext()?.MemoryOperation(Combine(ObjectIds.Of(target), fieldId), MemoryOpType.MemoryWrite);
    }

    public static void OnStaticFieldRead(int fieldId) =>
        FrayRuntime.ControlledContext()?.MemoryOperation(fieldId, MemoryOpType.MemoryRead);

    public static void OnStaticFieldWrite(int fieldId) =>
        FrayRuntime.ControlledContext()?.MemoryOperation(fieldId, MemoryOpType.MemoryWrite);

    private static int Combine(int objectHash, int fieldId) => 31 * (31 + objectHash) + fieldId;

    /// <summary>Stable (process-independent) hash for field identities.</summary>
    public static int StableHash(string value)
    {
        unchecked
        {
            var hash = (int)2166136261;
            foreach (var c in value)
            {
                hash = (hash ^ c) * 16777619;
            }
            return hash;
        }
    }
}
