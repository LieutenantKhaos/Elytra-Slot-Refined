# Elytra Slot! Refined

**Elytra Slot! Refined** is a maintained fork of Elytra Slot! focused on a cleaner,
more vanilla-looking survival inventory layout while preserving the mod's core
elytra behavior.

Maintained by **LieutenantKhaos**.

## What is refined?

The built-in elytra slot is integrated directly into the survival inventory:

- aligned with the vanilla chestplate row;
- aligned with the offhand/shield column;
- rendered with a vanilla-style slot recess;
- no external 32x32 side panel in the survival inventory;
- Creative inventory behavior remains separate.

## Core features

The upstream Elytra Slot! 3.0.0 architecture is retained, including:

- a dedicated elytra slot that leaves the chestplate slot available;
- vanilla-style flight, durability, Mending, and equipment behavior;
- support for modded gliders;
- Trinkets integration;
- Curios integration on NeoForge;
- Fabric and NeoForge support.

## Requirements

Minecraft **26.3**, Java **25**, Fabric Loader **0.20.1+**, and Fabric API **0.165.0+26.3**.

This branch provides the **1.1.0-beta.1 Fabric update**. Trinkets is optional:
the built-in slot works without it. Mod Menu is optional for the configuration UI.
The previous 26.2 release remains on `main`. NeoForge dependencies are aligned
for 26.3, but no NeoForge release is included in this beta.

Build: `gradlew.bat :fabric:build` (Java 25).

Validation: Fabric build, access widener, 23 injection signatures/callsites, and
dedicated-server initialization without accessory mods passed. Client inventory
interaction, flight, save migration, rendering and optional integrations still
need in-game testing.

The mod is designed to be installed on both the client and server because it
changes the player's inventory menu.

Optional integrations are detected at runtime.