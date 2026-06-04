# mod template
---
siscodeorg's minecraft mod template, requires knowledge of alphaflow to work well.

# Branch Info

Modloader: Fabric  
Minecraft Version: 1.20.1

## Core Principals
- Different Modloader/Version combinations will be served by separate trunk branches

Reasoning: Use supported and maintained tooling for whichever modloaders currently exist and will exist.  
Relying on third-parties for supporting shims or abstract wrappers will not work for our use case of just setting and
forgetting things. We shall only battle with gradle *once* and never again.

- Mojang mappings + Parchment whenever applicable.

Reasoning: Its just easier that way. The code will compile and be able to be referenced without doing constant remapping.  
Mixins will just apply. There's just no conversion step, knowledge will be transferable across versions and loaders.  
Porting becomes fixing merge conflicts.  

- Development happens once, in a canonical trunk branch.

## Package organization
These all will live on the "top-level" of the project.
All package names here assume a pre-existing module name, ex: `org.siscode.<modname>`, `xyz.alikind.<modname>`.

- `mixininterface`
Relates to interfaces to be injected using mixins.
- `mixin`
Relates to cross-loader mixins for any specific version.
- `platform.facade`
Relates to cross-loader, vanilla-accessing abstractions that can be reused across loaders.  
Most of the logic should be here.
- `platform.{fabric,forgelike}`
Relates to loader specific code. Here is where mod initializers live.  

## How to use
1. Clone this repository and navigate to the appropriate branch for the modloader, usually fabric, that development will be based on, for the required version you're making a mod for.

2. Replace the following mod metadata:

- On `gradle.properties`: `archive-base-name`, `mod_id`, `maven_group` (if desired).
- On `fabric.mod.json`: `name` and `description`.

3. Use IntelliJ IDEA to move the package from `org.siscode.template` to your package.
4. Run `:build`. 
