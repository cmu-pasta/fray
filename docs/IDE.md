# Use Fray with Intellij

Fray runs smoothly with Intellij IDEA. However, by default the IDE calls `toString` to display the value of 
local variables. This may trigger unnecssary schedules in Fray (e.g., if the `toString` method contains concurrency 
primitives). This makes Fray replay nondeterministic.

To disable Intellij `toString` evaluation, 
go to `File -> Settings -> Build, Execution, Deployment -> Debugger -> Data Views -> Java` uncheck `Enable toString 
object view`.
