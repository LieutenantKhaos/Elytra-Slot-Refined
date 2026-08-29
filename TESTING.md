# Elytra Slot 3.0.0 — manual test plan

Three host modes exist. Phase A runs with no accessory mod (the default dev run),
Phase B adds Trinkets on Fabric, Phase C adds Curios on NeoForge. Phase A is the
big one; B and C mostly re-check the seams.

Useful setup for every phase: creative mode, `/give @s elytra`, and a second
elytra for the exclusivity checks. For flight, jump off a block and press jump
again while falling.

---

## Phase A — built-in slot (vanilla BODY equipment), no accessory mods

### A1. The slot and its art
- [ ] Open the inventory. A 32×32 panel sits immediately left of the GUI, with a
      one-pixel gap between panel and inventory border, and an 18×18 slot recess
      centred in it. It should look like it was cut from the vanilla inventory
      texture — no seams, no colour mismatch, and note the highlight pixel in the
      top-left and shadow pixel in the bottom-right of the panel interior.
- [ ] Empty slot shows the elytra icon.
- [ ] Toggle the recipe book open and closed. The panel moves with the GUI and
      stays glued to its left edge.

### A2. Equipping — every gesture
- [ ] Right-click an elytra in hand → it lands in the slot, plays the **elytra**
      equip sound (not the generic armour sound), wings appear on your back.
- [ ] Right-click again with a *different* elytra in hand → they swap; the old
      one goes to your hand.
- [ ] Drag an elytra onto the slot with the mouse → equips.
- [ ] Shift-click an elytra in your inventory → jumps into the slot.
- [ ] **Shift-click the elytra in the slot** → returns to inventory. *(This was
      broken before the review — it did nothing.)*
- [ ] Left-click the equipped elytra to pick it up onto the cursor, then click it
      back in. *(Also previously broken: clicks on the panel were being treated as
      "drop item".)* Nothing should ever fall on the ground.
- [ ] Press Q over the slot → throws the elytra on the ground.
- [ ] Hover the slot and press a number key (1–9) → swaps with that hotbar slot.
- [ ] Hover the slot and press F → swaps with the offhand.
- [ ] With a *second* elytra in your inventory and one already equipped,
      shift-click the spare → it should move between inventory and hotbar
      normally, never vanish or dead-click.

### A3. Exclusivity — only one elytra at a time
- [ ] Elytra in the mod slot: try to put a second elytra in the **chest armour
      slot** (drag, and shift-click) → refused both ways.
- [ ] Chestplate in the chest slot + elytra in the mod slot → allowed, both work.
- [ ] Elytra in the chest slot: the mod slot refuses an elytra.
- [ ] Point a dispenser loaded with an elytra at yourself while wearing one in the
      mod slot, and power it → it must **not** equip to your chest. *(Fixed in
      review.)*

### A4. Flight, exactly like vanilla
- [ ] Glide from a height. Steering, speed and sound feel identical to a
      chest-equipped elytra.
- [ ] Firework rocket boost works while gliding.
- [ ] Land and glide repeatedly, checking durability on the tooltip: it should
      tick down at the same rate as a chest elytra (1 point per second of flight).
- [ ] Fly an elytra down to 1 durability → it stops working rather than breaking,
      same as vanilla. It must never break into nothing mid-air.
- [ ] Break an elytra deliberately (in the chest slot, for comparison) vs the mod
      slot: the break sound/particles and item disappearance should match.
- [ ] Enchant an elytra with Unbreaking → visibly lasts longer.

### A5. Enchantments and curses
- [ ] Mending elytra, damaged, in the mod slot → pick up XP orbs; it repairs.
- [ ] With both a damaged Mending chestplate and a damaged Mending elytra, XP
      should sometimes repair one and sometimes the other (no starvation, no
      double-repair).
- [ ] Curse of Binding on the elytra in the mod slot → in survival you cannot
      take it out (creative can).
- [ ] Curse of Vanishing on the elytra → die with keepInventory off; the elytra is
      destroyed, not dropped.
- [ ] A Protection-enchanted elytra in the mod slot reduces damage the same as it
      would in the chest slot.

### A6. Death, respawn, worlds
- [ ] `/gamerule keepInventory false`, die → the elytra drops with everything else
      and can be picked back up.
- [ ] `/gamerule keepInventory true`, die → still equipped after respawn.
- [ ] Go through a nether portal and back → still equipped.
- [ ] Die in the End / return via the end portal → still equipped.
- [ ] Log out and back in → still equipped, no equip sound on login.
- [ ] Log out **while gliding**, log back in → no stuck flight state.

### A7. Creative mode
- [ ] Open the creative **Inventory** tab. The elytra slot appears to the right of
      the armour column, mirroring the shield slot, with a proper slot frame.
- [ ] **Count your hotbar slots in that tab — all nine must be there**, and no
      phantom elytra should be drawn over any of them. *(This was broken before the
      review: one hotbar slot vanished and a ghost elytra appeared on another.)*
- [ ] Put an item in each hotbar slot first, then open the tab — every item shows
      in the right cell.
- [ ] Middle-click the equipped elytra → clones onto the cursor.
- [ ] Use the creative "destroy item" slot on the equipped elytra → removed cleanly.
- [ ] Switch tabs away and back → the slot is still correct, not duplicated.

### A8. Rendering
- [ ] Wings render on your back in third person, and animate when gliding.
- [ ] Wearing a cape: the cape is hidden while the elytra is equipped (vanilla
      behaviour), and the elytra uses your **cape texture** if you have a custom
      one — same as a chest elytra.
- [ ] Open to LAN and join with the second client (or use both clients on one
      world): **the other player's wings must render**, both standing and gliding.
- [ ] Unequip while the other player watches → wings disappear on their screen.
- [ ] Break the elytra mid-flight while watched → wings disappear for the observer
      too, not just for you.

### A9. Third-party mods (the two you asked about)
- [ ] Install a durability-HUD / equipment-display mod → it shows the elytra and
      its durability live, with no compat code on our side.
- [ ] Install a gravestone mod → die with keepInventory off; the elytra goes into
      the grave with everything else and is restored on reclaim.

### A10. Config
- [ ] Open the config screen (ModMenu on Fabric / mods list on NeoForge). The
      "show elytra slot in inventory" toggle is **greyed out** — with no accessory
      mod it is the only slot, so it cannot be hidden.
- [ ] The provider dropdown shows Auto / Built-in / Trinkets / Curios.

### A11. Migration from 2.x (only if you have an old world)
- [ ] Open a world last played with Elytra Slot 2.x that had an elytra equipped →
      the elytra appears in the new slot, nothing is lost.

---

## Phase B — Trinkets host (Fabric)

Copy the Trinkets jar from `testmods/` into `fabric/runs/client/mods/`, then relaunch.

- [ ] Log line at startup reads `active elytra host: trinkets`.
- [ ] The elytra equips into the Trinkets **chest/elytra** slot (open the Trinkets
      screen), and the slot shows our elytra icon and is named "Elytra".
- [ ] Right-click an elytra in hand → goes into the Trinkets slot.
- [ ] Flight, durability, mending and rendering all work — these are Trinkets' own
      implementations now, so watch for **doubled** behaviour: durability must not
      drain twice as fast, and there must be exactly one pair of wings rendered.
- [ ] Exclusivity still holds against the vanilla chest slot both ways.
- [ ] By default the mod's own inventory panel is **hidden**. Turn on "show elytra
      slot in inventory" in the config, restart the world, and it appears; the two
      slots must remain mutually exclusive.
- [ ] Death/respawn behaves (Trinkets owns drops here).

## Phase C — Curios host (NeoForge)

Copy the Curios jar from `testmods/` into `neoforge/run/mods/`, then relaunch.

- [ ] Log line reads `active elytra host: curios`.
- [ ] **Hover an elytra in your inventory** — anywhere, equipped or not. It must not
      crash. *(This was a hard crash before the review.)*
- [ ] The Curios screen (the button on the inventory) shows an **Elytra** slot with
      our icon.
- [ ] Right-click an elytra in hand → goes into the Curios slot, with the elytra
      equip sound (not the generic armour sound).
- [ ] Flight and durability work — these are ours in Curios mode.
- [ ] Mending repairs the elytra in the Curios slot.
- [ ] Wings render on your back, and on other players.
- [ ] If your pack has a generic "Curio" slot, an elytra must **not** be accepted
      there — only in the elytra slot.
- [ ] Exclusivity against the vanilla chest slot holds both ways.
- [ ] Death/respawn behaves (Curios owns drops here).

---

## What to report back

For anything that fails, the useful details are: which phase/checkbox, what you
expected vs saw, and the tail of `fabric/runs/client/logs/latest.log` or
`neoforge/run/logs/latest.log` if anything was logged.
