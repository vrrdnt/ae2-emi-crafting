# AE2 EMI Integration
*(previously known as "AE2 EMI Crafting")*
---
Mod that allows AE2 versions without native EMI support to properly integrate with EMI.

> [!NOTE]
> Behavior here *may* change when compared to the official integration.
> Although efforts with parity will be made it is not guaranteed to look alike.

> [!WARN]
> Behavior is significantly different from the old mod.
> This is still a pre-alpha, and we follow a different modding mentality then before. 

## Branch Information
This is a completely rewritten version of the mod under a simplified build system.

The old mod was done at a time when my knowledge of the underlying systems weren't as good as they are now.
Support for other modloaders and versions will come once fabric 1.20.1 reaches a desired level of maturity.

## Goals
1. No dependencies other than AE2 and EMI. External mods like AE2WTLib will be supported as an optional dependency.
2. Deep integration with other mods through mixin plugins.
3. It does as little as possible. The mod should be lean and work well with heavier non-optimized features being behind a config option.
4. Optional server-side integration for complex features. The mod works "well enough" as a client-side only addon whenever possible, but it should be installed on the server for deeper integration.
