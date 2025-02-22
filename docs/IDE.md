# Fray Debugger Plugin

<!-- Plugin description -->
Fray Idea Plugin allows you to control concurrent programs in your Intellij IDE!
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Fray Debugger"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/26623-fray-debugger) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/26623-fray-debugger/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Usage

Fray Debugger Plugin provides an interactive interface to control the execution of concurrent programs. To start 
Fray Debugger, you may select `Debug (Fray)` from the Run/Debug Configurations dropdown:

![img.png](images/fray-plugin-launch.png)

Once the test is launched, Fray Debugger Plugin will pause the program execution at each synchronization point. You can 
then control the execution of the program using the Fray Debugger Plugin interface:

![img.png](images/fray-panel.png)

The dropdown manu shows all threads in the program. You can select a thread to view its stack trace.

![img.png](images/fray-dropdown.png)

In the editor, Fray also shows the current line of execution and the thread name.

![img.png](images/fray-editor.png)

Once you have decided which thread to schedule, you can click the `Schedule` button to schedule the selected thread.
Note that a thread may note be scheduled if it is blocked or if it is not ready to run. Such a thread will 
be marked as `Paused`.

![img.png](images/thread-paused.png)


# Launch Fray Test Without Plugin (Normal concurrency tests)

You can launch Fray tests from the IDE and debug them as you would with any other JUnit test.

![fray-idea.png](./images/fray-idea.png)

Select `frayTest` from the Gradle tool window to run all Fray tests in the project.


> [!WARNING]
> Fray debugger plugin only works with tests annotated with `@ConcurrencyTest` right now.


### Disable `toString` evaluation in debug mode

By default Intellij calls `toString` to display the value of 
local variables. This may trigger unnecssary schedules in Fray (e.g., if the `toString` method contains concurrency 
primitives). This makes Fray replay nondeterministic.

To disable Intellij `toString` evaluation, 
go to `File -> Settings -> Build, Execution, Deployment -> Debugger -> Data Views -> Java` uncheck `Enable toString 
object view`.
