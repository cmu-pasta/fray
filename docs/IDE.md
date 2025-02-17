# Use Fray with Intellij

Fray runs smoothly with Intellij IDEA. You can launch Fray tests from the IDE and debug them as you would with any other JUnit test.

![fray-idea.png](./images/fray-idea.png)

Select `frayTest` from the Gradle tool window to run all Fray tests in the project.





### Disable `toString` evaluation in debug mode

By default Intellij calls `toString` to display the value of 
local variables. This may trigger unnecssary schedules in Fray (e.g., if the `toString` method contains concurrency 
primitives). This makes Fray replay nondeterministic.

To disable Intellij `toString` evaluation, 
go to `File -> Settings -> Build, Execution, Deployment -> Debugger -> Data Views -> Java` uncheck `Enable toString 
object view`.
