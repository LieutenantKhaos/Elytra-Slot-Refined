# Elytra Slot Refined

**Elytra Slot Refined** is a maintained fork of Elytra Slot focused on a cleaner,
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

The upstream Elytra Slot 3.0.0 architecture is retained, including:

- a dedicated elytra slot that leaves the chestplate slot available;
- vanilla-style flight, durability, Mending, and equipment behavior;
- support for modded gliders;
- Trinkets integration;
- Curios integration on NeoForge;
- Fabric and NeoForge support.

## Requirements

Minecraft **26.2**.

The mod is designed to be installed on both the client and server because it
changes the player's inventory menu.

Optional integrations are detected at runtime.

## Building

This project uses the included Gradle wrapper.

Windows:

```bat
gradlew.bat clean build
```

Linux/macOS:

```sh
./gradlew clean build
```

Build metadata is centralized in `gradle.properties`.

## Project identity

- Mod name: **Elytra Slot Refined**
- Mod ID: `elytra_slot_refined`
- Version: `1.0.0`
- Maintainer: **LieutenantKhaos**
- License: **MIT**

The Java package namespace remains `com.warwa.elytraslot` intentionally. It is part
of the inherited code structure and does not indicate current project ownership.

## Attribution

This project is a modified fork of **Elytra Slot**.

Original code:
- Copyright (c) 2026 fmpapierz
- Licensed under the MIT License

Refined modifications:
- Copyright (c) 2026 LieutenantKhaos
- Licensed under the MIT License

The original copyright and license notice are preserved in `LICENSE`.
