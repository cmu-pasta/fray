using Fray.Interception;
using Mono.Cecil;
using Mono.Cecil.Cil;
using Mono.Cecil.Rocks;

namespace Fray.Rewriter;

public sealed class RewriterOptions
{
    public required string InputPath { get; init; }

    public required string OutputPath { get; init; }

    /// <summary>Renames the rewritten assembly (avoids identity clashes when the original is also loaded).</summary>
    public string? NewAssemblyName { get; init; }

    /// <summary>Insert scheduling points before accesses to fields declared in the assembly.</summary>
    public bool InstrumentMemoryAccesses { get; init; } = true;
}

public sealed class RewriteResult
{
    public required string OutputPath { get; init; }
    public int RedirectedCalls { get; init; }
    public int InstrumentedFieldAccesses { get; init; }
}

/// <summary>
/// Rewrites an assembly so its concurrency operations run under the Fray
/// engine: calls to <see cref="Monitor"/>, <see cref="Thread"/> (including
/// constructors), and <see cref="Interlocked"/> are redirected to the
/// <c>Fray.Interception</c> shims, and accesses to fields declared in the
/// assembly get scheduling-point hooks. This is the .NET analog of the JVM
/// implementation's bytecode instrumentation agent; outside a Fray run the
/// shims fall through to the original BCL behavior.
/// </summary>
public static class AssemblyRewriter
{
    public static RewriteResult Rewrite(RewriterOptions options)
    {
        var redirects = BuildRedirectMap();
        using var assembly = AssemblyDefinition.ReadAssembly(options.InputPath);
        if (options.NewAssemblyName != null)
        {
            assembly.Name.Name = options.NewAssemblyName;
            assembly.MainModule.Name = options.NewAssemblyName + ".dll";
        }
        var module = assembly.MainModule;

        var redirected = 0;
        var instrumented = 0;
        foreach (var type in module.GetTypes())
        {
            foreach (var method in type.Methods)
            {
                if (!method.HasBody)
                {
                    continue;
                }
                ProcessMethod(method, module, redirects, options.InstrumentMemoryAccesses,
                    ref redirected, ref instrumented);
            }
        }

        assembly.Write(options.OutputPath);
        return new RewriteResult
        {
            OutputPath = options.OutputPath,
            RedirectedCalls = redirected,
            InstrumentedFieldAccesses = instrumented,
        };
    }

    private static void ProcessMethod(MethodDefinition method, ModuleDefinition module,
        IReadOnlyDictionary<string, System.Reflection.MethodBase> redirects, bool instrumentMemory,
        ref int redirected, ref int instrumented)
    {
        var body = method.Body;
        body.SimplifyMacros();
        var il = body.GetILProcessor();

        foreach (var instruction in body.Instructions)
        {
            if (instruction.OpCode.Code is not (Code.Call or Code.Callvirt or Code.Newobj) ||
                instruction.Operand is not MethodReference target)
            {
                continue;
            }
            if (redirects.TryGetValue(SignatureKey(target), out var shim))
            {
                // Instance methods and constructors become static shim calls
                // taking the instance / returning it: identical stack effect.
                instruction.OpCode = OpCodes.Call;
                instruction.Operand = module.ImportReference(shim);
                redirected++;
            }
            else if (TryRedirectGenericCall(instruction, target, module))
            {
                redirected++;
            }
        }

        // Skip constructors: hooks would observe partially constructed
        // objects (and `this` before the base call is not verifiable).
        if (instrumentMemory && !method.IsConstructor)
        {
            foreach (var instruction in body.Instructions.ToList())
            {
                if (TryInstrumentFieldAccess(body, il, module, instruction))
                {
                    instrumented++;
                }
            }
        }

        body.OptimizeMacros();
    }

    private static System.Reflection.MethodInfo ShimMethod(Type type, string name, int genericArity,
        Func<System.Reflection.MethodInfo, bool>? filter = null) =>
        type.GetMethods().Single(m =>
            m.Name == name &&
            m.GetGenericArguments().Length == genericArity &&
            (filter == null || filter(m)));

    /// <summary>
    /// Redirects the generic Task/async members that the exact-signature map
    /// cannot express. Shim generic arguments are the declaring type's
    /// arguments followed by the call's own method arguments.
    /// </summary>
    private static bool TryRedirectGenericCall(Instruction instruction, MethodReference target,
        ModuleDefinition module)
    {
        var genericType = target.DeclaringType as GenericInstanceType;
        var elementName = genericType?.ElementType.FullName ?? target.DeclaringType.FullName;
        var genericCall = target as GenericInstanceMethod;

        System.Reflection.MethodInfo? shim = null;
        var genericArguments = new List<TypeReference>();

        switch (elementName)
        {
            case "System.Threading.Tasks.Task`1"
                when target.Name == "get_Result" && target.Parameters.Count == 0:
                shim = ShimMethod(typeof(Interception.ControlledTask), nameof(Interception.ControlledTask.Result), 1);
                genericArguments.Add(genericType!.GenericArguments[0]);
                break;

            // Task.Run<TResult>(Func<TResult>) — but not the Func<Task<TResult>>
            // unwrapping overload (async lambdas are handled by the builder shims).
            case "System.Threading.Tasks.Task"
                when target.Name == "Run" &&
                     genericCall is { GenericArguments.Count: 1 } &&
                     genericCall.ElementMethod.Parameters.Count == 1 &&
                     genericCall.ElementMethod.Parameters[0].ParameterType is GenericInstanceType funcType &&
                     funcType.ElementType.FullName == "System.Func`1" &&
                     funcType.GenericArguments[0] is GenericParameter:
                shim = ShimMethod(typeof(Interception.ControlledTask), nameof(Interception.ControlledTask.Run), 1);
                genericArguments.Add(genericCall.GenericArguments[0]);
                break;

            case "System.Runtime.CompilerServices.AsyncTaskMethodBuilder"
                when target.Name is "AwaitUnsafeOnCompleted" or "AwaitOnCompleted" &&
                     genericCall is { GenericArguments.Count: 2 }:
                shim = ShimMethod(typeof(Interception.ControlledAsync), target.Name, 2,
                    m => m.GetParameters()[0].ParameterType.FullName!.Contains("AsyncTaskMethodBuilder"));
                genericArguments.AddRange(genericCall.GenericArguments);
                break;

            case "System.Runtime.CompilerServices.AsyncTaskMethodBuilder`1":
                if (target.Name is "AwaitUnsafeOnCompleted" or "AwaitOnCompleted" &&
                    genericCall is { GenericArguments.Count: 2 })
                {
                    shim = ShimMethod(typeof(Interception.ControlledAsync), target.Name, 3);
                    genericArguments.Add(genericType!.GenericArguments[0]);
                    genericArguments.AddRange(genericCall.GenericArguments);
                }
                else if (target.Name == "get_Task" && target.Parameters.Count == 0)
                {
                    shim = ShimMethod(typeof(Interception.ControlledAsync), "GetTask", 1);
                    genericArguments.Add(genericType!.GenericArguments[0]);
                }
                else if (target.Name == "SetResult" && target.Parameters.Count == 1)
                {
                    shim = ShimMethod(typeof(Interception.ControlledAsync), "SetResult", 1);
                    genericArguments.Add(genericType!.GenericArguments[0]);
                }
                else if (target.Name == "SetException" && target.Parameters.Count == 1)
                {
                    shim = ShimMethod(typeof(Interception.ControlledAsync), "SetException", 1);
                    genericArguments.Add(genericType!.GenericArguments[0]);
                }
                break;

            case "System.Threading.Tasks.TaskCompletionSource`1":
                if (target.Name == "get_Task" && target.Parameters.Count == 0)
                {
                    shim = ShimMethod(typeof(Interception.ControlledTask), "GetTask", 1);
                    genericArguments.Add(genericType!.GenericArguments[0]);
                }
                else if (target.Name is "SetResult" or "TrySetResult" && target.Parameters.Count == 1)
                {
                    shim = ShimMethod(typeof(Interception.ControlledTask), target.Name, 1);
                    genericArguments.Add(genericType!.GenericArguments[0]);
                }
                else if (target.Name is "SetException" or "TrySetException" &&
                         target.Parameters.Count == 1 &&
                         target.Parameters[0].ParameterType.FullName == "System.Exception")
                {
                    shim = ShimMethod(typeof(Interception.ControlledTask), target.Name, 1,
                        m => m.GetParameters()[1].ParameterType == typeof(Exception));
                    genericArguments.Add(genericType!.GenericArguments[0]);
                }
                break;
        }

        if (shim == null)
        {
            return false;
        }
        var redirected = new GenericInstanceMethod(module.ImportReference(shim));
        foreach (var argument in genericArguments)
        {
            redirected.GenericArguments.Add(argument);
        }
        instruction.OpCode = OpCodes.Call;
        instruction.Operand = redirected;
        return true;
    }

    private static bool TryInstrumentFieldAccess(MethodBody body, ILProcessor il, ModuleDefinition module,
        Instruction instruction)
    {
        var code = instruction.OpCode.Code;
        if (code is not (Code.Ldfld or Code.Stfld or Code.Ldsfld or Code.Stsfld))
        {
            return false;
        }
        var fieldReference = (FieldReference)instruction.Operand;
        var fieldDefinition = fieldReference.Resolve();
        // Only fields declared in the assembly being rewritten.
        if (fieldDefinition == null || fieldDefinition.Module != module)
        {
            return false;
        }
        // Instance hooks pass the target as object: only safe for classes
        // (a value-type target on the stack would need boxing).
        if (code is Code.Ldfld or Code.Stfld && fieldDefinition.DeclaringType.IsValueType)
        {
            return false;
        }
        // Stores need a temporary local of the field's type.
        if (code == Code.Stfld && fieldReference.FieldType.ContainsGenericParameter)
        {
            return false;
        }

        var fieldId = MemoryHooks.StableHash($"{fieldDefinition.DeclaringType.FullName}::{fieldDefinition.Name}");

        // Prefixes (volatile., unaligned.) must stay glued to their
        // instruction: insert the hook before the first prefix.
        var anchor = instruction;
        while (anchor.Previous != null && anchor.Previous.OpCode.OpCodeType == OpCodeType.Prefix)
        {
            anchor = anchor.Previous;
        }

        List<Instruction> hook;
        switch (code)
        {
            case Code.Ldfld:
            {
                // [obj] -> dup, push id, hook(obj, id) -> [obj]
                var onFieldRead = module.ImportReference(typeof(MemoryHooks).GetMethod(nameof(MemoryHooks.OnFieldRead))!);
                hook = new List<Instruction>
                {
                    il.Create(OpCodes.Dup),
                    il.Create(OpCodes.Ldc_I4, fieldId),
                    il.Create(OpCodes.Call, onFieldRead),
                };
                break;
            }
            case Code.Stfld:
            {
                // [obj, value] -> stash value, dup obj, hook, restore value.
                var onFieldWrite = module.ImportReference(typeof(MemoryHooks).GetMethod(nameof(MemoryHooks.OnFieldWrite))!);
                var temporary = new VariableDefinition(module.ImportReference(fieldReference.FieldType));
                body.Variables.Add(temporary);
                body.InitLocals = true;
                hook = new List<Instruction>
                {
                    il.Create(OpCodes.Stloc, temporary),
                    il.Create(OpCodes.Dup),
                    il.Create(OpCodes.Ldc_I4, fieldId),
                    il.Create(OpCodes.Call, onFieldWrite),
                    il.Create(OpCodes.Ldloc, temporary),
                };
                break;
            }
            case Code.Ldsfld:
            {
                var onStaticRead = module.ImportReference(typeof(MemoryHooks).GetMethod(nameof(MemoryHooks.OnStaticFieldRead))!);
                hook = new List<Instruction>
                {
                    il.Create(OpCodes.Ldc_I4, fieldId),
                    il.Create(OpCodes.Call, onStaticRead),
                };
                break;
            }
            default:
            {
                var onStaticWrite = module.ImportReference(typeof(MemoryHooks).GetMethod(nameof(MemoryHooks.OnStaticFieldWrite))!);
                hook = new List<Instruction>
                {
                    il.Create(OpCodes.Ldc_I4, fieldId),
                    il.Create(OpCodes.Call, onStaticWrite),
                };
                break;
            }
        }

        InsertBeforeWithRetarget(body, il, anchor, hook);
        return true;
    }

    /// <summary>
    /// Inserts <paramref name="hook"/> before <paramref name="anchor"/> and
    /// retargets branches and exception-handler boundaries that pointed at the
    /// anchor, so the hook executes as part of the anchored operation (and
    /// exclusive region ends keep excluding it).
    /// </summary>
    private static void InsertBeforeWithRetarget(MethodBody body, ILProcessor il, Instruction anchor,
        IReadOnlyList<Instruction> hook)
    {
        var first = hook[0];
        foreach (var instruction in hook)
        {
            il.InsertBefore(anchor, instruction);
        }
        foreach (var instruction in body.Instructions)
        {
            if (ReferenceEquals(instruction.Operand, anchor))
            {
                instruction.Operand = first;
            }
            else if (instruction.Operand is Instruction[] targets)
            {
                for (var i = 0; i < targets.Length; i++)
                {
                    if (ReferenceEquals(targets[i], anchor))
                    {
                        targets[i] = first;
                    }
                }
            }
        }
        foreach (var handler in body.ExceptionHandlers)
        {
            if (handler.TryStart == anchor)
            {
                handler.TryStart = first;
            }
            if (handler.TryEnd == anchor)
            {
                handler.TryEnd = first;
            }
            if (handler.HandlerStart == anchor)
            {
                handler.HandlerStart = first;
            }
            if (handler.HandlerEnd == anchor)
            {
                handler.HandlerEnd = first;
            }
            if (handler.FilterStart == anchor)
            {
                handler.FilterStart = first;
            }
        }
    }

    private static string SignatureKey(MethodReference method) =>
        $"{method.DeclaringType.FullName}::{method.Name}({string.Join(",", method.Parameters.Select(p => p.ParameterType.FullName))})";

    private static string SignatureKey(System.Reflection.MethodBase method) =>
        $"{method.DeclaringType!.FullName}::{method.Name}({string.Join(",", method.GetParameters().Select(p => p.ParameterType.FullName))})";

    private static Dictionary<string, System.Reflection.MethodBase> BuildRedirectMap()
    {
        var map = new Dictionary<string, System.Reflection.MethodBase>();

        void Redirect(System.Reflection.MethodBase? from, Type shimType, string shimName, Type[] shimParameters)
        {
            var shim = shimType.GetMethod(shimName, shimParameters)
                ?? throw new InvalidOperationException($"Shim {shimType.Name}.{shimName} not found.");
            map[SignatureKey(from ?? throw new InvalidOperationException("BCL method not found."))] = shim;
        }

        var obj = typeof(object);
        var objRef = typeof(object).MakeByRefType();
        var boolRef = typeof(bool).MakeByRefType();
        var intRef = typeof(int).MakeByRefType();
        var longRef = typeof(long).MakeByRefType();
        var span = typeof(TimeSpan);
        var monitor = typeof(Monitor);
        var thread = typeof(Thread);
        var interlocked = typeof(Interlocked);
        var cm = typeof(ControlledMonitor);
        var ct = typeof(ControlledThread);
        var ci = typeof(ControlledInterlocked);

        // Monitor (covers the C# `lock` statement pattern).
        Redirect(monitor.GetMethod("Enter", new[] { obj }), cm, "Enter", new[] { obj });
        Redirect(monitor.GetMethod("Enter", new[] { obj, boolRef }), cm, "Enter", new[] { obj, boolRef });
        Redirect(monitor.GetMethod("Exit", new[] { obj }), cm, "Exit", new[] { obj });
        Redirect(monitor.GetMethod("TryEnter", new[] { obj }), cm, "TryEnter", new[] { obj });
        Redirect(monitor.GetMethod("TryEnter", new[] { obj, typeof(int) }), cm, "TryEnter", new[] { obj, typeof(int) });
        Redirect(monitor.GetMethod("TryEnter", new[] { obj, span }), cm, "TryEnter", new[] { obj, span });
        Redirect(monitor.GetMethod("TryEnter", new[] { obj, boolRef }), cm, "TryEnter", new[] { obj, boolRef });
        Redirect(monitor.GetMethod("TryEnter", new[] { obj, typeof(int), boolRef }), cm, "TryEnter", new[] { obj, typeof(int), boolRef });
        Redirect(monitor.GetMethod("Wait", new[] { obj }), cm, "Wait", new[] { obj });
        Redirect(monitor.GetMethod("Wait", new[] { obj, typeof(int) }), cm, "Wait", new[] { obj, typeof(int) });
        Redirect(monitor.GetMethod("Wait", new[] { obj, span }), cm, "Wait", new[] { obj, span });
        Redirect(monitor.GetMethod("Pulse", new[] { obj }), cm, "Pulse", new[] { obj });
        Redirect(monitor.GetMethod("PulseAll", new[] { obj }), cm, "PulseAll", new[] { obj });

        // Thread constructors and lifecycle.
        Redirect(thread.GetConstructor(new[] { typeof(ThreadStart) }), ct, "Create", new[] { typeof(ThreadStart) });
        Redirect(thread.GetConstructor(new[] { typeof(ThreadStart), typeof(int) }), ct, "Create", new[] { typeof(ThreadStart), typeof(int) });
        Redirect(thread.GetConstructor(new[] { typeof(ParameterizedThreadStart) }), ct, "Create", new[] { typeof(ParameterizedThreadStart) });
        Redirect(thread.GetConstructor(new[] { typeof(ParameterizedThreadStart), typeof(int) }), ct, "Create", new[] { typeof(ParameterizedThreadStart), typeof(int) });
        Redirect(thread.GetMethod("Start", Type.EmptyTypes), ct, "Start", new[] { thread });
        Redirect(thread.GetMethod("Start", new[] { obj }), ct, "Start", new[] { thread, obj });
        Redirect(thread.GetMethod("Join", Type.EmptyTypes), ct, "Join", new[] { thread });
        Redirect(thread.GetMethod("Join", new[] { typeof(int) }), ct, "Join", new[] { thread, typeof(int) });
        Redirect(thread.GetMethod("Interrupt", Type.EmptyTypes), ct, "Interrupt", new[] { thread });
        Redirect(thread.GetMethod("Sleep", new[] { typeof(int) }), ct, "Sleep", new[] { typeof(int) });
        Redirect(thread.GetMethod("Sleep", new[] { span }), ct, "Sleep", new[] { span });
        Redirect(thread.GetMethod("Yield", Type.EmptyTypes), ct, "Yield", Type.EmptyTypes);

        // Task-parallel subset (async/await stays real and fails fast on waits).
        var task = typeof(System.Threading.Tasks.Task);
        var cta = typeof(ControlledTask);
        Redirect(task.GetMethod("Run", new[] { typeof(Action) }), cta, "Run", new[] { typeof(Action) });
        Redirect(task.GetMethod("Wait", Type.EmptyTypes), cta, "Wait", new[] { task });
        Redirect(task.GetMethod("Wait", new[] { typeof(int) }), cta, "Wait", new[] { task, typeof(int) });
        Redirect(task.GetMethod("WaitAll", new[] { typeof(System.Threading.Tasks.Task[]) }), cta, "WaitAll", new[] { typeof(System.Threading.Tasks.Task[]) });
        Redirect(task.GetProperty("IsCompleted")!.GetGetMethod(), cta, "IsCompleted", new[] { task });
        Redirect(task.GetMethod("Delay", new[] { typeof(int) }), cta, "Delay", new[] { typeof(int) });
        Redirect(task.GetMethod("Delay", new[] { span }), cta, "Delay", new[] { span });
        Redirect(task.GetMethod("WhenAll", new[] { typeof(System.Threading.Tasks.Task[]) }), cta, "WhenAll", new[] { typeof(System.Threading.Tasks.Task[]) });
        Redirect(task.GetMethod("WhenAll", new[] { typeof(IEnumerable<System.Threading.Tasks.Task>) }), cta, "WhenAll", new[] { typeof(IEnumerable<System.Threading.Tasks.Task>) });

        // Async machinery: instance calls on builder/awaiter struct addresses
        // become static shim calls taking them by ref (same stack shape).
        var asyncBuilder = typeof(System.Runtime.CompilerServices.AsyncTaskMethodBuilder);
        var asyncBuilderRef = asyncBuilder.MakeByRefType();
        var controlledAsync = typeof(ControlledAsync);
        Redirect(asyncBuilder.GetProperty("Task")!.GetGetMethod(), controlledAsync, "GetTask", new[] { asyncBuilderRef });
        Redirect(asyncBuilder.GetMethod("SetResult", Type.EmptyTypes), controlledAsync, "SetResult", new[] { asyncBuilderRef });
        Redirect(asyncBuilder.GetMethod("SetException", new[] { typeof(Exception) }), controlledAsync, "SetException", new[] { asyncBuilderRef, typeof(Exception) });
        var taskAwaiter = typeof(System.Runtime.CompilerServices.TaskAwaiter);
        Redirect(taskAwaiter.GetMethod("GetResult", Type.EmptyTypes), controlledAsync, "GetResult", new[] { taskAwaiter.MakeByRefType() });

        // TaskCompletionSource (the generic variant goes through the
        // structural generic redirect).
        var taskCompletionSource = typeof(System.Threading.Tasks.TaskCompletionSource);
        Redirect(taskCompletionSource.GetProperty("Task")!.GetGetMethod(), cta, "GetTask", new[] { taskCompletionSource });
        Redirect(taskCompletionSource.GetMethod("SetResult", Type.EmptyTypes), cta, "SetResult", new[] { taskCompletionSource });
        Redirect(taskCompletionSource.GetMethod("TrySetResult", Type.EmptyTypes), cta, "TrySetResult", new[] { taskCompletionSource });
        Redirect(taskCompletionSource.GetMethod("SetException", new[] { typeof(Exception) }), cta, "SetException", new[] { taskCompletionSource, typeof(Exception) });
        Redirect(taskCompletionSource.GetMethod("TrySetException", new[] { typeof(Exception) }), cta, "TrySetException", new[] { taskCompletionSource, typeof(Exception) });

        // Interlocked.
        Redirect(interlocked.GetMethod("Increment", new[] { intRef }), ci, "Increment", new[] { intRef });
        Redirect(interlocked.GetMethod("Increment", new[] { longRef }), ci, "Increment", new[] { longRef });
        Redirect(interlocked.GetMethod("Decrement", new[] { intRef }), ci, "Decrement", new[] { intRef });
        Redirect(interlocked.GetMethod("Decrement", new[] { longRef }), ci, "Decrement", new[] { longRef });
        Redirect(interlocked.GetMethod("Add", new[] { intRef, typeof(int) }), ci, "Add", new[] { intRef, typeof(int) });
        Redirect(interlocked.GetMethod("Add", new[] { longRef, typeof(long) }), ci, "Add", new[] { longRef, typeof(long) });
        Redirect(interlocked.GetMethod("Exchange", new[] { intRef, typeof(int) }), ci, "Exchange", new[] { intRef, typeof(int) });
        Redirect(interlocked.GetMethod("Exchange", new[] { longRef, typeof(long) }), ci, "Exchange", new[] { longRef, typeof(long) });
        Redirect(interlocked.GetMethod("Exchange", new[] { objRef, obj }), ci, "Exchange", new[] { objRef, obj });
        Redirect(interlocked.GetMethod("CompareExchange", new[] { intRef, typeof(int), typeof(int) }), ci, "CompareExchange", new[] { intRef, typeof(int), typeof(int) });
        Redirect(interlocked.GetMethod("CompareExchange", new[] { longRef, typeof(long), typeof(long) }), ci, "CompareExchange", new[] { longRef, typeof(long), typeof(long) });
        Redirect(interlocked.GetMethod("CompareExchange", new[] { objRef, obj, obj }), ci, "CompareExchange", new[] { objRef, obj, obj });

        return map;
    }
}
