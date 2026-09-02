# AE2 EMI Crafting — Forge 1.20.1

This is a downstream Forge port of [blocovermelho/ae2-emi-crafting](https://github.com/blocovermelho/ae2-emi-crafting). It adds EMI synthetic-favorite crafting controls to Applied Energistics 2 crafting terminals, with Monifactory as the primary compatibility target.

The port patches the EMI integration already present in Monifactory's AE2 build. It does not register a second set of AE2 recipes or replace AE2's terminal UI.

## Supported versions

| Component | Supported version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.4.13 |
| Applied Energistics 2 | 15.4.10-cosmolite.36 |
| EMI | 1.1.22+1.20.1+forge |
| Monifactory | 0.13.7 and 0.13.8 |

Monifactory 0.13.7 and 0.13.8 use the same Forge, EMI, and AE2/MoniLabs dependency stack relevant to this mod. The AE2 version is deliberately pinned so an incompatible upstream handler change fails clearly instead of silently corrupting a transfer.

## What it changes

While an AE2 crafting terminal is open, the mod:

- exposes the terminal's stored item inventory to EMI's craftability and synthetic-favorite calculations;
- honors EMI's requested batch count for **craft one** and **craft all**;
- supports crafting directly to the cursor or player inventory;
- fills the crafting grid with the requested number of batches when no output destination is requested;
- applies to crafting-terminal subclasses, including AE2 wireless crafting terminals that use the same handler; and
- validates and performs all inventory extraction and crafting on the server.

EMI remains a client-side dependency, but **AE2 EMI Crafting must be installed on both the client and server**. Single-player automatically supplies the server half through the integrated server.

## Installation

1. Download the regular JAR from this repository's [Releases](https://github.com/vrrdnt/ae2-emi-crafting/releases) page. Do not use the sources JAR.
2. Put it in the client's `mods` directory.
3. Put the same JAR in the server's `mods` directory when playing multiplayer.

Use EMI's configured sidebar actions for craft one, craft all, craft to cursor, and craft to inventory. No additional key bindings are added by this mod.

A plain recipe `+` click fills empty ingredient slots with one item and preserves matching stacks already in the grid, following AE2's normal behavior. Shift-clicking `+` bulk-fills the grid, balancing identical ingredients across their repeated recipe slots. For example, nine glass blocks across three glass slots become `3 / 3 / 3`, not `7 / 1 / 1`. Extra items that cannot fill another complete set of those slots remain in storage. Existing items are preserved and rebalanced, so an indivisible remainder already in the grid can leave counts one apart. Different ingredients (including non-stackable tools) have independent stack limits and availability.

## Scope and tradeoffs

Installing this mod opts crafting terminals into full stored-network exposure to EMI, even when AE2's `exposeInventoryToEmi` option is disabled. This is necessary for synthetic favorites to see ME-stored ingredients, but very large networks may make EMI's craftable calculations more expensive.

Only items that are actually stored in the ME network, crafting grid, cursor, or player inventory count as available. An item that is merely autocraftable from an AE2 pattern is not advertised to EMI as if it already existed, and this mod does not automatically schedule those missing ingredients. A single **craft all** action is bounded to one crafting-grid stack (at most 64 recipe batches, and less for smaller stack sizes or output constraints).

Pattern-terminal virtual ingredient encoding from [AE2 issue #8074](https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/8074) is outside this Forge port's current scope.

## Building

The Gradle wrapper provisions the build toolchains. The resulting runtime JAR targets Java 17.

```shell
./gradlew build
```

The build also runs regression tests for balanced ingredient allocation, stack limits, partial extraction, and item conservation. To run just the tests, use `./gradlew test`.

Artifacts are written to `build/libs/`.

The GitHub workflow builds every push and pull request targeting `forge/1.20.1`. Every successful branch push publishes its head commit's runtime JAR to a new GitHub Release; pull requests only upload an Actions artifact. Manually running the workflow on `forge/1.20.1` also publishes a release. A push containing multiple commits produces one release for the head commit.

Branch builds automatically add the workflow run number to the patch component of `mod_version`. For example, base version `0.1.0` and run number `3` produce version `0.1.3`, tag `v0.1.3`, and JAR `ae2emi-forge-0.1.3.jar`. The same version is embedded in the mod metadata. Version numbers can have gaps because pull requests, failed builds, and manually tagged builds also consume run numbers. Re-running an existing run reuses its version and tag, preserving already-published JARs and retrying any missing upload. No version-bump commit or manually pushed tag is needed.

Tags beginning with `v` still publish explicit versions (for example, `v0.2.0` builds version `0.2.0`). Reserve automatic versions for the workflow; it refuses to overwrite a tag pointing to a different commit. To start a new major/minor release line, update `mod_version` in `gradle.properties`; its patch component remains the offset added to the workflow run number.

## Fork attribution and license

The original project and history are by **siscodeorg / blocovermelho**. This branch is maintained as a downstream Forge port by **vrrdnt** and is not an official Applied Energistics 2, EMI, or Monifactory project. Issues specific to this port should be reported in this fork rather than upstream.

The original [MIT license](LICENSE.txt) and copyright notice are preserved unchanged. Forge-port contributions are distributed under the same license.
