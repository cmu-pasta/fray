using System;
using System.Threading;
using Fray.Core;

namespace Fray.Test
{
    class Program
    {
        private static object _lock = new object();
        private static int _counter = 0;

        static void Main(string[] args)
        {
            Console.WriteLine("--- Fray POC Start ---");

            // Initialize Fray Runtime to hook up with Profiler
            Runtime.Initialize();

            Thread t1 = new Thread(Worker);
            Thread t2 = new Thread(Worker);

            t1.Start("Thread 1");
            t2.Start("Thread 2");

            t1.Join();
            t2.Join();

            Console.WriteLine($"Final Counter: {_counter}");
            Console.WriteLine("--- Fray POC End ---");
        }

        static void Worker(object? name)
        {
            for (int i = 0; i < 5; i++)
            {
                FrayMonitor.Enter(_lock);
                try
                {
                    int temp = _counter;
                    Thread.Sleep(10); // Artificial delay to provoke races if not locked
                    _counter = temp + 1;
                    Console.WriteLine($"{name}: Incremented to {_counter}");
                }
                finally
                {
                    FrayMonitor.Exit(_lock);
                }
            }
        }
    }
}
