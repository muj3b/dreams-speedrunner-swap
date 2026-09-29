# SpeedrunnerSwap 4.3.7 compatibility

Verified September 28, 2026. One release JAR, compiled against the oldest supported
Paper API with Java 21 bytecode. Tests and API libraries are not bundled in it.

## Supported targets and evidence

| Target | Server Java | Verification |
| --- | --- | --- |
| Published Paper 1.21.x releases, listed below | 21 | Packaged-JAR linkage on every listed API; 42 behavioral tests on matching 1.21.11 MockBukkit |
| Paper 26.1.1 | 25 | Packaged-JAR linkage (the pinned API is alpha) |
| Paper 26.1.2 and 26.2 | 25 | Packaged-JAR linkage plus 42 behavioral tests on each matching MockBukkit version |
| Paper 26.3 build 135 beta | 25 | Packaged-JAR linkage only; no matching MockBukkit release available |

The 15 targets are **1.21, 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7,
1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1.1, 26.1.2, 26.2, and 26.3**.
Exact immutable Maven coordinates are in [verification/api-versions.txt](verification/api-versions.txt).
The live Paper project listing did not include 1.21.2 or 26.1 server releases, so
those are not invented matrix entries. “1.21+” does not promise future releases.
Spigot, Folia, Fabric, Forge, and Bedrock are not supported targets.

All checks are **code-only**. No Minecraft client, actual Paper server, playtest,
or third-party-plugin interoperability test was run. A linkage pass finds missing
classes/members and static/interface mismatches, but cannot establish server
semantics, reflection behavior, world conversion correctness, or gameplay quality.
MockBukkit simulates server behavior; it is not the server implementation.
There is no claim of zero possible bugs. In particular, 26.3 remains beta/API-only
verified: the 26.2 mock fails to initialize 26.3's registries before plugin loading,
so that unsupported combination is not counted as a behavioral pass.

## What changed

- Minimum plugin API is now `1.21`; the main code compiles against the pinned 1.21 API.
- Spawn, limbo, and session-world lookup prefer stable namespaced world keys, with
  legacy world-name fallback. Existing configs and hunter groups remain intact.
- Potion lookup uses the registry, retaining legacy aliases; potion menus iterate
  the registry instead of relying on an old static enumeration helper.
- Adventure text conversion preserves legacy colors in menus, titles, and action
  bars. The same release code is exercised with Adventure 4 and 5.
- Kits accept namespaced material names and report unsupported entries independently.
  Invalid stacks or one invalid armor slot do not discard other valid entries.
- Generic material tasks unavailable on the current server are excluded from the
  assignment pool with a warning, without rewriting saved definitions. Complex/manual
  objectives are unchanged. Re-enabling an unsupported task does not bypass the guard.
- Existing independent hunter-body mechanics remain unchanged: each group rotates
  its own controllers and state while a single permanent runner can remain active.
- Documentation no longer advertises the existing Simple Voice Chat placeholder
  as functional auto-muting. No voice integration was added in this release.

## Upgrading

1. Stop the server and back up the **entire server/worlds and plugin data** before
   changing Minecraft/Paper versions. Keep a separate pre-upgrade backup: do not
   open upgraded worlds with an older server version.
2. Use the appropriate Java runtime above. Replace the old SpeedrunnerSwap JAR with
   `speedrunnerswap-4.3.7.jar`; do not leave two plugin versions installed.
3. Keep your existing `config.yml`, kits, and `tasks.yml`. Names still work; setting
   spawn/limbo through the plugin also saves `world_key`. When manually choosing a
   different world by name, update/remove a previously saved key because it wins.
4. Let Paper handle its own world-layout conversion. This plugin does not move
   world folders or migrate third-party world/inventory data.
5. Read startup warnings for unavailable materials. Definitions remain saved so
   moving forward to a server that has those materials can make them available again.

## Repeatable code-only verification

Prerequisites: Maven and JDK 21/25. No Minecraft download, server boot, EULA
acceptance, or manual gameplay is part of these commands.

```sh
# Use JDK 21 for the release build and 1.21.11 mock tests:
mvn --batch-mode clean verify

# Switch JAVA_HOME to JDK 25, then run:
bash verification/check-behavior-26.sh
bash verification/check-apis.sh
```

The behavior script compiles **only tests** against each matching 26.x API and runs
them against the already-built release classes. It asserts that both the release
JAR hash and all main-class/resource hashes are unchanged. Do not build the release
with `mvn clean package -Ptest-26` or a newer `paper.version` override: that would
invalidate the baseline compilation evidence.

The linkage script reads the same release JAR against each isolated API classpath.
It checks referenced classes, field/method descriptors, inherited member resolution,
static/instance and class/interface changes, method handles, bytecode level, package
contents, and plugin API floor. Five fault-injection tests check that the verifier
rejects missing members/types and static/interface mismatches and accepts inheritance.
It does not replace the JVM verifier or check all possible runtime access semantics.

The 42 behavioral tests cover group independence, handoffs, deaths/disconnects,
pause/resume, command/menu configuration, cleanup, previous modes, task metadata,
world-key/config preservation, colors, potion aliases, health attributes, invalid
kits, and unsupported material task filtering. Reports are under
`target/surefire-reports`, `target/reports-26.1.2`, and `target/reports-26.2`.
CI repeats this procedure and uploads the release JAR and reports after successful
checks. API/verifier reports are under `verification/target`.

## Primary research

- [Minecraft Java 26.3 release](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3)
- [Paper project versions](https://fill.papermc.io/v3/projects/paper) and
  [26.3 latest-build channel](https://fill.papermc.io/v3/projects/paper/versions/26.3/builds/latest)
- [Paper Java requirements](https://docs.papermc.io/paper/getting-started/)
- [Paper 26.1 changes: world layout, clocks, API versions](https://papermc.io/news/26-1/)
- [Paper 26.2 changes: Adventure 5 and API removals](https://papermc.io/news/26-2/)
- [Paper Maven metadata](https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/maven-metadata.xml)
- [MockBukkit published modules](https://repo.maven.apache.org/maven2/org/mockbukkit/mockbukkit/)
- [Surefire test classpath configuration](https://maven.apache.org/surefire/maven-surefire-plugin/examples/configuring-classpath.html)
