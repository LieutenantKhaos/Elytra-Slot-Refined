# Vanilla Parity Audit — elytraslot multiloader (MC 26.1.2)

Supersedes `HANDOFF.md`. Dated 2026-04-19.

Scope: every behavioral divergence from vanilla's CHEST-slotted elytra lifecycle. Code under `common/src/main/java/com/warwa/elytraslot/**`. Vanilla references are from bytecode inspection of `minecraft-client.jar` 26.1.2.

---

## 2026-04-19 Update — deviations G1, G2, F, UI2, UI3, UI4, UI5, SE2, C1, C2 FIXED

Implemented and verified at runtime. See commit and `RESEARCH.md` for exact method signatures and Fabric/NeoForge API surface.

- **G1 (attribute modifiers + location-based enchantment effects)**: `ElytraEquipEffects.onSlotChanged` now mirrors both `LivingEntity.onEquipItem` (sound + GameEvent) AND the tick-driven `collectEquipmentChanges` pipeline (stop-OLD + apply-NEW modifiers and location-based effects). Runtime verified: `onSlotChanged ENTRY → playing equip sound → gameEvent=EQUIP → stopped OLD modifiers → applied NEW modifiers → runLocationChangedEffects → firing CriteriaTriggers.INVENTORY_CHANGED → broadcasting sync payload → EXIT`.
- **F (remote-player sync)**: `ElytraSlotSyncPayload` record + `ElytraSyncDispatcher` loader-agnostic hook. Fabric: `PayloadTypeRegistry.clientboundPlay().register(...)` + `ServerPlayNetworking.send(p, payload)` for each `PlayerLookup.tracking(entity)` + self. NeoForge: `RegisterPayloadHandlersEvent.registrar("1").optional().playToClient(...)` + `PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload)`. Client handler writes via `setItemSilent` so existing render-state extract picks up remote-player stacks. Runtime verified: `FabricDispatcher.broadcastSlotChange sent=1 → Fabric S2C receive → SyncClient.handle writing stack into player`.
- **G2 (Equippable.swappable)**: `ElytraEquipMixin` now returns early if `inHandEquippable.swappable()` is false, defending against any caller that bypasses `Item.use`'s existing swappable gate.
- **UI2 (Curse of Binding)**: custom slot's `Slot.mayPickup` override checks `EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE` (NOT `PREVENT_EQUIPMENT_DROP` which is Curse of Vanishing) + `!player.isCreative()`, matching vanilla `ArmorSlot.mayPickup` exactly.
- **UI3 / UI4 / UI5 (shift-click parity)**: `InventoryMenuQuickMoveMixin` rewritten to use vanilla's `moveItemStackTo(stack, start, end, reverse)` helper which already handles stack merging (pass 1, no `mayPlace`) and single-empty-slot fill (pass 2, uses `mayPlace`). Vanilla slot-search order: main-inventory-first (9..36), hotbar-last (36..45), matching vanilla's behaviour for armor slots.
- **SE2 (server-side validation)**: `mayPlace` is now the single authority for placement — both server-side and client-side paths flow through `moveItemStackTo` which enforces it.
- **C1 (Curios / Trinkets detection)**: both loaders detect via `ModList.get().isLoaded("curios")` (NeoForge) / `FabricLoader.getInstance().isModLoaded("trinkets")` (Fabric) and WARN-log if present. Deeper interop deferred.
- **C2 (advancement trigger)**: `CriteriaTriggers.INVENTORY_CHANGED.trigger(serverPlayer, inventory, newStack)` fires server-side whenever our slot changes. This is what vanilla's "Suit Up" / "Cover Me With Diamonds" advancements use.

Explicit `[elytraslot] ...` INFO log lines were added at every new code path so the run can be verified. Runtime sample from 2026-04-19:

```
[Server thread] onSlotChanged ENTRY old=0 minecraft:air new=1 minecraft:elytra firstTick=false silent=false creative=false
[Server thread] onSlotChanged playing equip sound=...equip_elytra
[Server thread] onSlotChanged gameEvent=...equip
[Server thread] onSlotChanged OLD stack empty — skipped stop phase
[Server thread] onSlotChanged applied NEW modifiers count=0 stack=1 minecraft:elytra
[Server thread] onSlotChanged runLocationChangedEffects NEW stack=1 minecraft:elytra
[Server thread] onSlotChanged firing CriteriaTriggers.INVENTORY_CHANGED for Player341
[Server thread] onSlotChanged broadcasting sync payload to trackers of Player341
[Server thread] FabricDispatcher.broadcastSlotChange player=Player341 stack=1 minecraft:elytra sent=1
[Server thread] onSlotChanged EXIT
[Render thread] Fabric S2C receive playerId=19487181-... stack=1 minecraft:elytra
[Render thread] SyncClient.handle writing stack=1 minecraft:elytra into player=Player341 containerId=349271e3
```

See sections 2, 5 below for the original deviation table and prioritized fix list — those listings remain the research index; entries marked DEVIATES/BROKEN there are now all FIXED except known scope deferrals (F1 non-Player scope, D7 drop-order cosmetic, C1 Curios deep interop).

---

## 1. Executive Summary

Top risks and gaps (highest impact first):

- **Location-based enchantment effects never run for the custom-slot stack on equip** (only on unequip-via-break). Vanilla `LivingEntity.collectEquipmentChanges` drives both `EnchantmentHelper.runLocationChangedEffects` on new stacks and the per-modifier apply via `forEachModifier`. Because the custom slot is not in `EquipmentSlot.VALUES`, that code path never sees it. **Severity: HIGH** — any modded/datapack enchantment that applies on equip (attributes, auras) is silently disabled.
- **Attribute modifiers on the stack are not applied when equipped** (same root cause). Default vanilla elytra has none, so vanilla players notice nothing, but modded gliders with `ATTRIBUTE_MODIFIERS` component get dropped on the floor semantically. **Severity: MEDIUM-HIGH**.
- **No `ClientboundSetEquipmentPacket` for the custom slot** (known `F`). Remote clients never see this player's elytra either visually or for any attribute/effect sync. **Severity: HIGH** (cosmetic + gameplay).
- **Custom slot is not damaged by generic equipment-hurt paths** — specifically `LivingEntity.doHurtEquipment` (thorns, etc.) and `Player.hurtHelmet` never touch it. Vanilla chest elytras also aren't damaged by these (only head/full-armor loops), so this is PARITY by coincidence, but any future vanilla path that iterates `EquipmentSlot.VALUES` for damage will silently miss the custom slot. **Severity: LOW (today), HIGH (future-proofing)**.
- **`Equippable.swappable()` flag is not honored** on the right-click intercept — the mod intercepts any CHEST-slotted elytra-like item regardless. Modded items with `swappable=false` will still be swapped. **Severity: LOW**.
- **Dupe window in `InventoryMenuQuickMoveMixin`**: shift-click uses `sourceSlot.set(ItemStack.EMPTY)` + `target.setByPlayer(stack.copy())`. On a desynced client with concurrent interactions this is vulnerable, though container-level menu sync usually rejects it. **Severity: LOW**.
- **Curse of Binding is NOT honored for the custom slot**: the slot's `Slot` implementation has no `mayPickup` override. **Severity: MEDIUM** (gameplay deviation).
- **`updateFallFlying` damage ignores `PREVENT_EQUIPMENT_DROP`-style checks that vanilla path implicitly has via `hurtAndBreak`** — the mod's direct hurt path is fine, but an item with `Unbreakable` component bypasses damage like vanilla does via `hurtAndBreak`. PARITY.
- **`firstTick` guard is applied to the server side only**, matching vanilla. PARITY.
- **Right-click path does NOT call `player.setItemSlot` or produce vanilla's side-effect package**. Instead it manually stores into the container and calls `ElytraEquipEffects.onSlotChanged`. Result: sound + game event only, no modifier apply, no location-based effect run. Matches the collect-equipment-changes gap above. **Severity: MEDIUM-HIGH**.

Known-broken (previously documented):
- F: Remote player rendering absent (no packet sync).
- Bytecode-level confirmed: custom-slot stack excluded from `ClientboundSetEquipmentPacket`.

---

## 2. Deviation Table

| ID | Area | Deviation vs vanilla | Status | Evidence | Recommendation |
|----|------|----------------------|--------|----------|----------------|
| G1 | Equip | `setItemSlot` not called; no entry into `collectEquipmentChanges` → no `runLocationChangedEffects`, no `forEachModifier` apply | DEVIATES | `ElytraEquipMixin.java:101-110`, vanilla `LivingEntity.collectEquipmentChanges` lines 83-105 | Mirror `runLocationChangedEffects` and `forEachModifier` in `ElytraEquipEffects.onSlotChanged` |
| G2 | Equip | `Equippable.swappable()` flag ignored | DEVIATES | `ElytraEquipMixin.java:50-55` gates only on `slot()==CHEST` | Add `if (!inHandEquippable.swappable()) return;` check |
| G3 | Equip | `ElytraEquipEffects` fires `EQUIP`/`UNEQUIP` based on `newEquippable != null`, not on whether the OLD had one | PARITY | `ElytraEquipEffects.java:61` — matches vanilla `LivingEntity.onEquipItem` bytecode (line 110-133) | none |
| G4 | Equip | Silent-path writes (NBT load, respawn, death-drop clear) correctly suppress sound + game event | PARITY | `ElytraSlotContainer.java:45-52`, `PlayerElytraStorageMixin.java:62`, `ServerPlayerCloneMixin.java:51` | none |
| G5 | Equip | `isSameItemSameComponents` short-circuit present | PARITY | `ElytraEquipEffects.java:37` | none |
| G6 | Equip | `firstTick` guard present | PARITY | `ElytraEquipEffects.java:40` via `LivingEntityAccessor` | none |
| G7 | Equip | `isSpectator` skip present | PARITY | `ElytraEquipEffects.java:36` | none |
| G8 | Equip | `isClientSide` skip present — runs only server-side | PARITY | `ElytraEquipEffects.java:35` — matches vanilla bytecode line 0-17 | none |
| G9 | Equip | `player.isSilent()` guard present | PARITY | `ElytraEquipEffects.java:50` | none |
| G10 | Equip | `Equippable.equipSound()` used directly rather than `getEquipSound(slot, stack, equippable)` | PARITY (bytecode shows vanilla's `getEquipSound` just returns `equippable.equipSound()`) | `ElytraEquipEffects.java:54`; vanilla `LivingEntity.getEquipSound` bytecode just `areturn equippable.equipSound()` | none |
| F1 | Flight | `canGlide` hook only fires for `Player`, not other `LivingEntity` subclasses | DEVIATES (scope) | `ElytraSlotMixin.java:37` gates on `Player` | Intentional — no non-player has an elytra slot. Low risk. |
| F2 | Flight | `canGlide` gate checks onGround/isPassenger/LEVITATION manually | PARITY | `ElytraSlotMixin.java:40-42` — matches vanilla `LivingEntity.canGlide` |  |
| F3 | Flight | Damage order matches vanilla by substituting at CHEST position | PARITY | `ElytraSlotMixin.java:87-98` | none |
| F4 | Flight | Vanilla chest having an elytra makes custom slot dormant (early return) | PARITY-INTENTIONAL | `ElytraSlotMixin.java:54` | none |
| F5 | Flight | `onEquippedItemBroken` custom — strips modifiers only from the broken stack, not from `getItemBySlot(CHEST)` (which would be chestplate) | DEVIATES but INTENTIONAL | `ElytraSlotMixin.java:129-150` | none (correct) |
| F6 | Flight | Glide-start sound: vanilla has no specific "take-off" sound — PARITY confirmed | PARITY | vanilla `updateFallFlying` emits only `GameEvent.ELYTRA_GLIDE` every 10 ticks | none |
| F7 | Flight | Firework boosting: `FireworkRocketItem`/`Player` firework-use checks `isFallFlying`, which checks shared flag 7, which IS set by the mod's flight path | PARITY | shared flag 7 driven by `canGlide` return value and `ElytraSlotMixin.onCanGlide` setting return value | none |
| D1–D3 | Death | Documented in `HANDOFF.md` — fixed | PARITY | see `HANDOFF.md` | none |
| D4 | Death | `player.drop(stack, true, false)` matches `Inventory.dropAll`'s signature exactly | PARITY | `PlayerElytraStorageMixin.java:100`; vanilla `Inventory.dropAll` bytecode calls `player.drop(stack, true, false)` | none |
| D5 | Death | Hook injects at `@At("TAIL")` of `Player.dropEquipment` — AFTER vanilla's `destroyVanishingCursedItems` + `inventory.dropAll` path. Gamerule check in mod re-reads same gamerule | PARITY | `PlayerElytraStorageMixin.java:69-102` | none (slightly redundant branch) |
| D6 | Death | Custom slot's vanishing-curse destroy uses `setItemSilent`, whereas vanilla's `destroyVanishingCursedItems` uses `inventory.removeItemNoUpdate` | PARITY (both produce no sound) | `PlayerElytraStorageMixin.java:92` | none |
| D7 | Death | ORDER: Vanilla destroys cursed BEFORE dropping inventory. Mod's TAIL runs AFTER vanilla inventory has fully dropped. Order is irrelevant for correctness but differs | DEVIATES (cosmetic) | `PlayerElytraStorageMixin.java:69` is `TAIL` | none |
| P1 | Persistence | Key `elytraslot_item` unlikely to collide — namespaced | PARITY-SAFE | `PlayerElytraStorageMixin.java:50` | none |
| P2 | Persistence | If mod removed, vanilla `readAdditionalSaveData` silently ignores unknown keys (ValueInput.read returns Optional.empty if not present, but does not fail on unknown keys). Stack silently discarded. | ACCEPTABLE | — | none |
| P3 | Persistence | `ItemStack.CODEC` round-trips full component data (enchantments, `DYED_COLOR`, `ATTRIBUTE_MODIFIERS`, custom name) | PARITY | `PlayerElytraStorageMixin.java:50,59` | none |
| P4 | Persistence | Spectator transitions via `restoreFrom` carry stack | PARITY | `ServerPlayerCloneMixin.java:56-59` matches vanilla `ServerPlayer.restoreFrom` at bytecode branches 173-186 | none |
| R1 | Rendering | Elytra renders in first-person world only; never on remote clients | BROKEN (known F) | no packet sent — vanilla `ClientboundSetEquipmentPacket` is emitted from `LivingEntity.handleEquipmentChanges` bytecode offset 30-46, custom slot not in `EquipmentSlot.VALUES` so excluded | Send a custom packet via both loaders' networking APIs |
| R2 | Rendering | `CapeLayerMixin` hides cape at HEAD when custom-slot elytra exists, matching vanilla's behavior for chest-slot elytra | PARITY | `CapeLayerMixin.java:29-43` | none |
| R3 | Rendering | `@At("RETURN")` injection on `WingsLayer.submit` handles both early and final return paths | PARITY | `ElytraLayerMixin.java:56`; verified by bytecode: two return opcodes (at 36 and 119) | none |
| R4 | Rendering | Tint color `0` passed; invisible player is NOT explicitly gated — but vanilla `WingsLayer.submit` itself does not check `isInvisible` (that's handled at parent layer `RenderLayer.shouldRender` level) | PARITY | `ElytraLayerMixin.java:83`; vanilla bytecode line 111 is `iconst_0` | none |
| R5 | Rendering | Glint/armor-trim render: the mod passes the stack to `EquipmentLayerRenderer.renderLayers`, which handles both trim and glint | PARITY | `ElytraLayerMixin.java:72-84` | none |
| R6 | Rendering | Baby model selection via `state.isBaby` | PARITY | `ElytraLayerMixin.java:69` | none |
| R7 | Rendering | `getPlayerElytraTexture` inline copy of vanilla's private static method is byte-equivalent | PARITY | `ElytraLayerMixin.java:93-104`; vanilla bytecode lines 0-57 in `WingsLayer.getPlayerElytraTexture` | none |
| R8 | Rendering | Invisible-player check: vanilla chest-elytra renders invisible players' elytras too (no guard in `WingsLayer.submit`). The mod also doesn't guard. | PARITY | both pass-through | none |
| UI1 | Inventory | `mayPlace` blocks non-elytra-like items and blocks when chest has an elytra | PARITY-EXTENSION | `InventoryMenuMixin.java:27-29` | none |
| UI2 | Inventory | `Slot.mayPickup` NOT overridden — Curse of Binding allows removal; vanilla CHEST slot's `ArmorSlot.mayPickup` returns `!hasBinding(stack) \|\| creative` | DEVIATES | no override in `InventoryMenuMixin.java:24-36` | Override `mayPickup` to block pickup when stack has `EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP` or `BINDING_CURSE` on a non-creative player |
| UI3 | Inventory | Shift-click OUT loop iterates slots 9..44; does not include hotbar 0..8 preferentially. Vanilla `InventoryMenu.quickMoveStack` prefers hotbar last → this mod prefers main inventory. Minor behavior deviation. | DEVIATES (minor) | `InventoryMenuQuickMoveMixin.java:28-37` | Align with vanilla's hotbar-preference order |
| UI4 | Inventory | Shift-click OUT does not stack onto existing slots with the same item — only fills empty slots. If the player has a partial stack of the same item (e.g. modded non-max-1 gliders), this is wrong. | DEVIATES | `InventoryMenuQuickMoveMixin.java:30` | For non-unique stackable gliders, also fill existing matching stacks |
| UI5 | Inventory | Shift-click IN bypasses `mayPlace` — the `setByPlayer` call is not gated. Could let stacked or disallowed items in. | DEVIATES (low) | `InventoryMenuQuickMoveMixin.java:44-55` | Call `s.mayPlace(stack)` before placing |
| UI6 | Inventory | `hasClickedOutside` extends click region to include the panel, preventing item-drop when clicking in the panel but outside the slot | PARITY-EXTENSION | `InventoryScreenBgMixin.java:55-61` | none |
| EX1 | Equip exclusivity | `ArmorSlot.mayPlace` intercept blocks placing elytra in chest slot when custom slot already has one | PARITY-EXTENSION | `ElytraExclusiveMixin.java:17-30` | none |
| SE1 | Security | Shift-click dupe race: `sourceSlot.set(EMPTY)` + `target.setByPlayer(stack.copy())` — two writes, menu sync broadcasts each. Server-authoritative menu validation protects this, but note that `InventoryMenuQuickMoveMixin` uses `stack.copy()` TWICE (line 50-51). If `target.setByPlayer` triggers `ElytraEquipEffects.onSlotChanged` before `sourceSlot.set(EMPTY)` and something injects mid-call (e.g. an inventory listener creates another copy), there's a theoretical duplication surface. | LOW | `InventoryMenuQuickMoveMixin.java:49-51` | Compute once, do set-empty first, then set-target |
| SE2 | Security | No server-side validation that a stack shift-clicked onto the custom slot is elytra-like beyond client-side `mayPlace`. Client-authoritative placement. A modified client could put anything in the custom slot. | DEVIATES | `InventoryMenuQuickMoveMixin.java:42,44-55`; `Slot.mayPlace` is client-side only for a ServerPlayerGameMode path | Revalidate `isElytraLike` in the `setByPlayer` or call server `canPlace` |
| SE3 | Security | NBT malformation: `ItemStack.CODEC` returns `DataResult.error` on malformed input. `input.read("elytraslot_item", ItemStack.CODEC)` returns `Optional.empty()` → silently treated as no saved item, **not** a crash | PARITY-SAFE | `PlayerElytraStorageMixin.java:59` | none |
| C1 | Compat | Curios/Trinkets conflict — if another mod adds a curio slot for elytras, the player could theoretically have: custom-slot elytra + curio-slot elytra + chest-slot elytra. Flight-tick damage picker only picks one; but attribute modifiers, if any, stack. Rendering: `WingsLayer` and our injection would both try to draw. | DEVIATES (mod-compat) | — | Add a config option / detect known curio IDs |
| C2 | Compat | Adv. progression: `body_armor` / `chest_armor` advancements trigger from `setItemSlot(CHEST, ...)` pathway which the mod bypasses. Custom-slot equip does NOT grant elytra-wearing advancements. | DEVIATES | no `ServerPlayer.awardAdvancement` call in mod | If desired, trigger advancements manually |
| C3 | Compat | No `player.detectEquipmentUpdates()` call — the vanilla detection loop ignores custom slots | DEVIATES | vanilla `detectEquipmentUpdates` iterates `EquipmentSlot.VALUES` only | N/A — part of G1 |

---

## 3. Per-Category Findings

### 3.1 Equip lifecycle

Vanilla's equip is a two-hop pipeline:

1. **Interactive set** (`Player.setItemSlot(CHEST, stack)`): writes to `EntityEquipment`, then calls `onEquipItem(slot, old, new)` — which emits sound + `GameEvent.EQUIP`/`UNEQUIP` (confirmed by bytecode of `LivingEntity.setItemSlot`: calls `equipment.set(...)` then `onEquipItem(...)` only).
2. **Tick-detected sync** (`LivingEntity.tick` → `detectEquipmentUpdates` → `collectEquipmentChanges` → `handleEquipmentChanges`): runs on *any* slot change seen this tick. `collectEquipmentChanges` does THREE things per changed slot:
   - `stopLocationBasedEffects(old, slot, attributes)` on the old stack.
   - `forEachModifier(slot, (attr, mod) -> attributes.getInstance(attr).addTransientModifier(mod))` on the new stack — this is where attribute modifiers are applied.
   - `EnchantmentHelper.runLocationChangedEffects(serverLevel, stack, this, slot)` on the new stack.
   - Then `handleEquipmentChanges` broadcasts `ClientboundSetEquipmentPacket` to tracking clients.

The mod's `ElytraEquipEffects.onSlotChanged` only covers step 1 (sound + GameEvent). **Step 2 is entirely missed**. For vanilla elytra (no attribute modifiers, no location-based enchantment effects beyond `SWIFT_SNEAK` etc. which don't apply to chest) the practical impact is limited. For datapack / mod gliders with attribute modifiers or location-based enchantments, those will silently fail.

Files involved:
- `common/src/main/java/com/warwa/elytraslot/ElytraEquipEffects.java`
- `common/src/main/java/com/warwa/elytraslot/mixin/ElytraEquipMixin.java`
- `common/src/main/java/com/warwa/elytraslot/ElytraSlotContainer.java`

### 3.2 Flight lifecycle

The mod's `ElytraSlotMixin` hooks:
- `canGlide` at RETURN: if vanilla returns false and custom-slot has elytra-like, return true.
- `updateFallFlying` at HEAD (cancellable): when CHEST is empty and custom slot has elytra-like, run a vanilla-parity mirror.

Observations:
- The `canGlide` injection correctly mirrors vanilla's pre-gates (onGround, isPassenger, LEVITATION).
- `updateFallFlying` mirror correctly iterates `EquipmentSlot.VALUES` in order (A2 fix).
- `updateFallFlying` is fully replaced by the mod when custom slot supplies the glider — `ci.cancel()` after the server-only branch. Vanilla's `checkFallDistanceAccumulation()` is called explicitly. Parity verified.
- **Non-Player LivingEntity subclasses**: the mixin gates on `instanceof Player` so armor-stands / mobs with custom glider items can't use the custom slot. Intentional.
- `damageGlider` for custom-slot case: `hurtAndBreak` with a custom `onBreak` consumer which:
  - Broadcasts byte 50 (entityEventForEquipmentBreak(CHEST)) — matches vanilla.
  - Strips attribute modifiers of the pre-break snapshot (correctly scoped to the broken stack only).
  - Calls `EnchantmentHelper.stopLocationBasedEffects(preBreakSnapshot, player, CHEST)`.
- **Gap**: on equip the matching `runLocationChangedEffects` is never called (G1), so the stop-on-break is called for effects that were never started.

### 3.3 Death / drop / respawn

All three D-deviations from `HANDOFF.md` are confirmed fixed in current code. Bytecode comparison of `Player.dropEquipment` (at 0-35) shows vanilla order is:

1. `super.dropEquipment(level)` (LivingEntity — drops held items, etc.)
2. If `!keepInventory`: `destroyVanishingCursedItems()`
3. If `!keepInventory`: `inventory.dropAll()`

The mod injects at TAIL of `Player.dropEquipment`, so it runs AFTER (2) and (3). The mod re-gates on `KEEP_INVENTORY` (redundant but correct). It honors `PREVENT_EQUIPMENT_DROP` (vanishing curse) for parity with vanilla's `destroyVanishingCursedItems`. `player.drop(stack, true, false)` matches vanilla `Inventory.dropAll` bytecode (`true` = include name, `false` = no throwing player sender) — the signature is actually `drop(ItemStack, boolean dropAround, boolean includeThrower)`.

Respawn / clone: `ServerPlayerCloneMixin.elytraslot$shouldCarry` implements vanilla's tri-gate: `restoreAll || isSpectator || KEEP_INVENTORY`. Verified against `ServerPlayer.restoreFrom` bytecode (branches at 52, 173, 180). Parity confirmed.

### 3.4 Persistence

- NBT key `elytraslot_item`, namespaced; low collision risk.
- `ItemStack.CODEC` round-trips all components.
- Silent-set on load prevents spurious equip sound.
- Malformed NBT returns `Optional.empty`, graceful.
- **Uninstalled-mod scenario**: the key is ignored by vanilla readers. The stored item is silently dropped. Acceptable.

### 3.5 Client sync

- `ClientboundSetEquipmentPacket` never includes custom slot — confirmed by bytecode of `LivingEntity.collectEquipmentChanges` + `handleEquipmentChanges`, which iterate `EquipmentSlot.VALUES` only.
- Result: other clients never receive the custom-slot stack. Their `HumanoidRenderState` never has it populated. Their `ElytraLayerMixin` sees `holder.elytraslot_getAccessoriesElytra() == EMPTY` and does not render.
- The OWNING client's `HumanoidRenderState` is populated because `HumanoidRenderStateMixin` calls `((IElytraSlotPlayer) player).elytraslot_getElytraStack()` at render-state extraction — and for the owning player that reads straight from the server-synced container (which the server updates whenever the player's `InventoryMenu` is open). When the menu is CLOSED, the owning client's container state is stale; however, the RENDER state extract pulls from the server-side stack on the integrated-server case or last-received state on dedicated. Server-sent equip updates happen only via `ClientboundSetEquipmentPacket` or (on the owning client) via `ClientboundContainerSetContentPacket` when the inventory menu is open. So on a dedicated server with the menu closed, the owning client can have a stale view.

### 3.6 Rendering

`WingsLayer.submit` bytecode: two `return` opcodes (36 and 119). The `@At("RETURN")` fix fires before both. `CapeLayerMixin` hides cape at HEAD. Glint/trim/tint/skin are all passed to `EquipmentLayerRenderer.renderLayers` matching vanilla's single call.

One nuance: vanilla's `WingsLayer.submit(EntityRenderState, ...)` bridge overload casts to `HumanoidRenderState` and calls the concrete method. Our injection targets the concrete method only. Parity.

### 3.7 Compat

- **Curios** (if present): no awareness. If user has both mods, could double-apply attribute modifiers on modded gliders.
- **Advancements**: `body_armor`/`chest_armor` triggered only via `setItemSlot(CHEST, ...)`. Not granted by custom-slot equip.
- **Recipe books**: N/A (not tied to equip).
- **Chest-hat modpacks / data packs**: elytra-like items in non-elytra shape (any `GLIDER` component) pass through `isElytraLike`. Compatible.

### 3.8 Soundness

- Dupe surface: shift-click uses `stack.copy()` twice, but each write goes through menu sync which is server-authoritative. Low risk in practice.
- Server-side validation: `mayPlace` is used client-side only for UI state — a modified client can send arbitrary click packets. Server re-runs `quickMoveStack` but the mod's injection only gates on `isElytraLike` which is correct. For drag/drop into the slot, there is no explicit re-check.
- Crash-on-load: `ItemStack.CODEC.parse(...).result()` failure returns empty; `input.read(...)` wraps it as `Optional.empty()`. No crash path observed.

---

## 4. Expected Bugs (user-facing)

| Scenario | Symptom | Severity |
|----|----|----|
| Glide in survival to wear down an elytra; repair it; fly again | Works normally | — |
| Equip custom-slot elytra with a modded ATTRIBUTE_MODIFIERS component | Modifier NOT applied | MEDIUM |
| Equip custom-slot elytra with an enchantment having location-based effects | Effect NOT started | MEDIUM |
| Equip a bound-to-body (Curse of Binding) elytra into custom slot | Can still remove it | MEDIUM |
| Another player on the same server sees you | Your elytra is INVISIBLE to them | HIGH (cosmetic) |
| Die wearing Curse-of-Vanishing elytra with keepInventory OFF | Elytra destroyed, correct | — |
| Die with keepInventory ON | Elytra kept, correct | — |
| Use /gamemode spectator then /gamemode survival | Elytra preserved via restoreFrom, correct | — |
| End-portal return | Elytra preserved via restoreFrom(restoreAll=true), correct | — |
| Shift-click out of custom slot with full inventory | Item disappears (no return to hotbar prefer order), only fills empty main slots 9..44 | MEDIUM |
| Shift-click modded stackable glider into slot (count > 1) | Enters as full stack rather than count=1 | LOW |
| Modded mod adds a non-CHEST-slotted glider (e.g. BACK) | Falls through to vanilla — correct | — |
| Modded item with `swappable=false` right-clicked | Still swaps into custom slot (mod bug, G2) | LOW |
| Trigger `body_armor` advancement by wearing elytra | NOT triggered when via custom slot | LOW |
| Persist world with elytra in custom slot, remove mod, reload | Elytra silently discarded | — |
| Concurrent rapid swap between chest and custom slot | Server-menu sync prevents dupe in practice | — |
| Custom-slot elytra broken mid-flight | Break VFX + sound fire correctly; enchantments stopped via snapshot | — |
| Fireworks glide boost with custom-slot elytra | Works (shared-flag 7 path) | — |

---

## 5. Prioritized Fix List

Ordered by impact-per-hour:

1. **G1 — Attribute modifiers + location-based enchantment effects on equip.**
   - *Where*: `ElytraEquipEffects.onSlotChanged`.
   - *What*: after sound + GameEvent dispatch, if server-side: for the OLD stack, call `EnchantmentHelper.stopLocationBasedEffects(old, player, CHEST)` and iterate `old.forEachModifier(CHEST, (attr, mod) -> attributes.getInstance(attr).removeModifier(mod))` — mirroring `LivingEntity.stopLocationBasedEffects`. For the NEW stack, iterate `new.forEachModifier(CHEST, (attr, mod) -> attributes.getInstance(attr).addTransientModifier(mod))` and call `EnchantmentHelper.runLocationChangedEffects(serverLevel, new, player, CHEST)`.
   - *Impact*: closes the biggest parity gap for datapack/mod gliders.

2. **F (known) — Remote player sync packet.**
   - *Where*: loader entry points + `ElytraEquipEffects.onSlotChanged`.
   - *What*: register a custom S2C payload (Fabric Networking API + NeoForge payload registrar) containing `playerId, ItemStack.STREAM_CODEC`. Broadcast to tracking players whenever `onSlotChanged` fires on the server. Client handler writes into the target player's `HumanoidRenderState` (or a keyed map from UUID → stack that the render state lookup consults).
   - *Impact*: fixes rendering for all non-owning clients.

3. **UI4 — Stackable shift-click.**
   - *Where*: `InventoryMenuQuickMoveMixin`.
   - *What*: when shifting OUT, first try to stack onto existing slots with matching item; then empty. When shifting IN for a count > 1 stack, only consume 1.
   - *Impact*: basic stackable correctness.

4. **UI2 — Curse of Binding respect.**
   - *Where*: `InventoryMenuMixin.elytraslot$addElytraSlot` — override `Slot.mayPickup`.
   - *What*: `return creative || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP);` (or a binding-curse equivalent).
   - *Impact*: survival gameplay parity.

5. **UI3 — Shift-click hotbar preference order.**
   - *Where*: `InventoryMenuQuickMoveMixin`.
   - *What*: mirror vanilla `InventoryMenu.quickMoveStack` slot-search order.
   - *Impact*: UX polish.

6. **G2 — Honor `Equippable.swappable()`.**
   - *Where*: `ElytraEquipMixin.onSwapWithEquipmentSlot`.
   - *What*: early-return after the `slot() != CHEST` check: `if (!inHandEquippable.swappable()) return;`.
   - *Impact*: modded-item correctness.

7. **SE2 — Server-side validation on shift-click/drag.**
   - *Where*: `InventoryMenuQuickMoveMixin` and `InventoryMenuMixin`'s `Slot.mayPlace`.
   - *What*: re-run `isElytraLike` on the server side path; reject non-elytra placement from any source.
   - *Impact*: defense-in-depth.

8. **UI5 — Call `mayPlace` before `setByPlayer` in shift-click IN.**
   - *Where*: `InventoryMenuQuickMoveMixin.java:50`.
   - *What*: insert `if (!s.mayPlace(stack)) return;`.
   - *Impact*: belt-and-braces.

9. **C2 — Trigger `body_armor` / `chest_armor` advancements.**
   - *Where*: `ElytraEquipEffects.onSlotChanged` — server-side only, after equip.
   - *What*: locate vanilla's advancement trigger (likely `CriteriaTriggers.EQUIPPED_ITEM` or similar) and fire it with the new stack.
   - *Impact*: completion polish.

10. **C1 — Curios/Trinkets detection.**
    - *Where*: optional module, runtime class-check for `top.theillusivec4.curios.api.CuriosApi`.
    - *What*: if Curios loaded, defer to Curios. Or provide a config.
    - *Impact*: interop in modpacks.

---

## Appendix: Bytecode Evidence Index

| Vanilla class / method | Ref path | Key offsets |
|---|---|---|
| `LivingEntity.onEquipItem` | from `minecraft-client.jar` | 0-17 client/spectator skip, 18-34 same-components/firstTick skip, 46-110 sound, 110-136 game event |
| `LivingEntity.setItemSlot` | | 0-15: equipment.set + onEquipItem |
| `LivingEntity.updateFallFlying` | | 0-112; chain: checkFallDistanceAccumulation → server-side → canGlide → shared-flag 7 → 10-tick mod damage |
| `LivingEntity.collectEquipmentChanges` | | 0-109: per-slot change detect + stopLocationBasedEffects; 109-229: apply modifiers via forEachModifier + runLocationChangedEffects |
| `LivingEntity.handleEquipmentChanges` | | 23-46: ClientboundSetEquipmentPacket broadcast |
| `Player.dropEquipment` | | 0-35: super + keepInv-guarded destroyVanishing + dropAll |
| `Player.destroyVanishingCursedItems` | | 0-54: inventory-only, uses removeItemNoUpdate |
| `Player.drop(ItemStack, boolean)` | | delegates to 3-arg form with false/boolean |
| `Inventory.dropAll` | | calls player.drop(stack, true, false) |
| `ServerPlayer.restoreFrom` | | 52 restoreAll branch; 173-186 keepInv||spectator branch |
| `Equippable.swapWithEquipmentSlot` | | 0-25 outer gate; 26-63 inner gate; 88-151 count<=1 branch; 152-203 count>1 branch |
| `WingsLayer.submit(HumanoidRenderState, ...)` | | return offsets at 36 (early) and 119 (late) |
| `WingsLayer.getPlayerElytraTexture` | | 0-57: AvatarRenderState cast, skin.elytra() then skin.cape()+showCape |

End of audit.
