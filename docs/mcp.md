# Fray MCP Server

Fray now ships with a Model Context Protocol (MCP) server that lets an LLM orchestrate thread
scheduling.

## Getting Started

> [!NOTE]
> Download the Fray repo and build the MCP server before proceeding. Follow README.md for the
> detailed build steps.

Understanding how the MCP server fits into Fray helps explain its design. The server runs as a
standalone process and communicates with the Fray runtime through Java Remote Method Invocation
(RMI), keeping the runtime free of extra dependencies. To use the MCP workflow you must launch your
tests with Fray and start the MCP server so they can talk to each other.

### Launch Tests with Fray

You can launch tests through the provided `bin` wrappers or via your IDE with additional arguments.

To run tests from the command line:
```shell
./bin/fray --mcp -cp CLASS_PATH CLASS_NAME
```

To run tests from IntelliJ, add the following VM options to the `Run/Debug Configuration`:
```shell
-Dfray.debugger=mcp -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

These flags instruct Fray to use [the remote scheduler](https://github.com/cmu-pasta/fray/blob/1368e31b7d66128d09bb994b37be01abc1aadb78/core/src/main/kotlin/org/pastalab/fray/core/scheduler/FrayIdeaPluginScheduler.kt#L11)
and to wait until a remote scheduler becomes available before starting the session.

Once the test run is waiting for a scheduler, start the MCP server to attach to it.

### Start the Fray MCP Server

The MCP server communicates over
[stdio](https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#stdio). Start it
through an MCP-capable agent such as [Codex](https://openai.com/index/introducing-codex/) or
[Claude Code](https://www.claude.com/product/claude-code).

1. Navigate to the directory that contains the tests you launched with Fray.
2. Register the Fray MCP server with your agent:
   ```shell
   claude mcp add --transport stdio Fray PATH_TO_JAVA25 -jar PATH_TO_FRAY_REPO/mcp/build/libs/fray-mcp-0.6.10-SNAPSHOT-all.jar
   ```
3. Start the agent and verify the connection by running `/mcp`.
4. Use the built-in prompt to have the LLM drive scheduling:
   ```bash
   /fray:find_concurrency_bug (MCP) NAME_OF_THE_CLASS
   ```

The available tools and prompts are documented in
[`SchedulerServer`](https://github.com/cmu-pasta/fray/blob/main/mcp/src/main/kotlin/org/pastalab/fray/mcp/SchedulerServer.kt).
