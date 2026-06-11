using Fray.Rewriter;

if (args.Length == 0 || args[0] is "-h" or "--help")
{
    Console.WriteLine("Usage: fray-rewrite <assembly.dll> [-o <output.dll>] [--name <new-assembly-name>] [--no-memory]");
    Console.WriteLine();
    Console.WriteLine("Redirects Monitor/Thread/Interlocked usage to the Fray engine and inserts");
    Console.WriteLine("scheduling points before field accesses. The rewritten assembly requires");
    Console.WriteLine("Fray.dll next to it at run time.");
    return args.Length == 0 ? 1 : 0;
}

var input = args[0];
string? output = null;
string? name = null;
var memory = true;
for (var i = 1; i < args.Length; i++)
{
    switch (args[i])
    {
        case "-o" or "--output":
            output = args[++i];
            break;
        case "--name":
            name = args[++i];
            break;
        case "--no-memory":
            memory = false;
            break;
        default:
            Console.Error.WriteLine($"Unknown option: {args[i]}");
            return 1;
    }
}
output ??= Path.Combine(
    Path.GetDirectoryName(Path.GetFullPath(input)) ?? ".",
    Path.GetFileNameWithoutExtension(input) + ".fray.dll");

var result = AssemblyRewriter.Rewrite(new RewriterOptions
{
    InputPath = input,
    OutputPath = output,
    NewAssemblyName = name,
    InstrumentMemoryAccesses = memory,
});
Console.WriteLine($"Rewrote {input} -> {result.OutputPath}");
Console.WriteLine($"  redirected calls:            {result.RedirectedCalls}");
Console.WriteLine($"  instrumented field accesses: {result.InstrumentedFieldAccesses}");
return 0;
