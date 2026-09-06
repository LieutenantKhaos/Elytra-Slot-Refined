# Changelog

## 1.0.0 — Elytra Slot Refined

Initial release of **Elytra Slot Refined**, maintained by **LieutenantKhaos**.

This release is based on the Elytra Slot 3.0.0 codebase and keeps its core
equipment, flight, compatibility, and multi-loader behavior while refining the
survival inventory presentation.

### Refined in this fork

- Moved the built-in survival elytra slot into the vanilla inventory itself.
- Aligned the elytra slot with the chestplate row.
- Aligned the elytra slot with the vanilla offhand/shield column.
- Replaced the external 32x32 side panel with a vanilla-style 18x18 slot recess.
- Kept Creative inventory behavior separate and unchanged.
- Centralized the fork's name, author, version, description, and license metadata
  through `gradle.properties`.
- Renamed the release identity to **Elytra Slot Refined** with mod ID
  `elytra_slot_refined`.

### Core behavior retained from upstream

- Dedicated elytra equipment without occupying the chestplate slot.
- Vanilla-style flight and durability behavior.
- Modded glider support.
- Trinkets compatibility.
- Curios compatibility on NeoForge.
- Fabric and NeoForge builds from the shared multiloader codebase.
- Existing upstream equipment-host and migration architecture.

### Attribution

Elytra Slot Refined is a modified fork of **Elytra Slot**. The upstream code is
copyright (c) 2026 fmpapierz and was released under the MIT License.

Refined modifications are copyright (c) 2026 LieutenantKhaos and are distributed
under the same MIT License. See `LICENSE` for the full license text.
