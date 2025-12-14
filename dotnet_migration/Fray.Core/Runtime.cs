using System;
using System.Runtime.InteropServices;
using System.Threading;
using System.Collections.Concurrent;

namespace Fray.Core
{
    public static class Runtime
    {
        // Must match the signature in C++: void (*)(unsigned long long, int)
        [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
        public delegate void OnThreadEventCallback(ulong threadId, int eventType);

        [DllImport("FrayProfiler", CallingConvention = CallingConvention.Cdecl)]
        public static extern void SetManagedCallback(IntPtr callback);

        private static OnThreadEventCallback _callbackDelegate; // Keep reference to prevent GC
        private static ConcurrentDictionary<ulong, bool> _activeThreads = new ConcurrentDictionary<ulong, bool>();

        public static void Initialize()
        {
            Console.WriteLine("[Fray.Core] Initializing Runtime...");
            _callbackDelegate = new OnThreadEventCallback(OnThreadEvent);

            // We need to pass the function pointer to the native profiler
            // Note: In a real scenario, the profiler is loaded by the runtime, so we need to find it or it calls us.
            // But here we are simulating the connection or assuming we can load the same DLL.
            // PROBLEM: The Profiler loaded by CoreCLR (via environment variable) is effectively a different "module"
            // if we DllImport it again, UNLESS the OS loader shares the handle.
            // On Linux, dlopen with RTLD_GLOBAL might help, but typically CoreCLR loads it privately.

            // HOWEVER, for this POC, we can try to rely on the OS dynamic linker to resolve symbols if the path is the same.
            // If that fails, we might not see the "SetManagedCallback" call affect the instance running as the profiler.
            //
            // Alternative strategy for POC:
            // The Profiler is a singleton. If we load the shared object, we get the same static variables?
            // Yes, usually on Linux if loaded from the same path.

            try
            {
                IntPtr ptr = Marshal.GetFunctionPointerForDelegate(_callbackDelegate);
                SetManagedCallback(ptr);
                Console.WriteLine("[Fray.Core] Callback registered with Profiler.");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[Fray.Core] Failed to register callback: {ex.Message}");
                Console.WriteLine("[Fray.Core] Ensure libFrayProfiler.so is in LD_LIBRARY_PATH or local directory.");
            }
        }

        private static void OnThreadEvent(ulong threadId, int eventType)
        {
            string evt = eventType == 1 ? "Created" : "Destroyed";
            Console.WriteLine($"[Fray.Core] MANAGED RECEIVED EVENT: Thread {threadId} {evt}");

            if (eventType == 1)
                _activeThreads.TryAdd(threadId, true);
            else
                _activeThreads.TryRemove(threadId, out _);
        }

        public static void OnMonitorEnter(object obj)
        {
            // Simple scheduler hook
            // In a real scheduler, we would pause execution here if needed.
            Console.WriteLine($"[Fray.Core] Thread {Thread.CurrentThread.ManagedThreadId} requesting lock on {obj.GetHashCode()}");
        }

        public static void OnMonitorExit(object obj)
        {
            Console.WriteLine($"[Fray.Core] Thread {Thread.CurrentThread.ManagedThreadId} released lock on {obj.GetHashCode()}");
        }
    }

    public static class FrayMonitor
    {
        public static void Enter(object obj)
        {
            Runtime.OnMonitorEnter(obj);
            System.Threading.Monitor.Enter(obj);
        }

        public static void Exit(object obj)
        {
            System.Threading.Monitor.Exit(obj);
            Runtime.OnMonitorExit(obj);
        }
    }
}
