# Changelog

## 3.0.0 — full rewrite

Elytra Slot 3.0.0 is a rewrite from scratch. The mod does the same thing it always
did — an elytra slot that leaves your chestplate alone — but the way it stores the
elytra changed completely, and almost everything else follows from that one
decision.

Existing worlds migrate automatically: an elytra saved by 2.x is moved into the new
slot the first time the world loads. Nothing is lost.

---

### The core change: a real equipment slot

**2.x** kept the elytra in a custom one-slot container bolted onto the player. No
vanilla system knew that container existed, so every behavior an equipped item
normally gets for free had to be re-implemented by hand:

- a custom network packet, because vanilla's equipment sync only carries real slots
- custom NBT save/load
- custom death-drop and respawn-copy handling
- a hand-written copy of vanilla's equip pipeline (sound, game events, attribute
  modifiers, enchantment effects)
- a hand-written copy of vanilla's entire in-flight durability method
- a custom hook to make Mending see the elytra
- per-mod compatibility code so gravestone mods could find it

**3.0.0** stores the elytra in the vanilla `BODY` equipment slot — a real slot that
players never otherwise use. It is genuine equipment, so vanilla handles the
persistence, the client sync, death drops, `keepInventory`, Curse of Vanishing,
Curse of Binding, attribute modifiers, enchantment effects, Mending and Unbreaking
through the exact code paths a chest-slot elytra uses. All of the machinery listed
above is simply gone.

What remained was small: one hook so a chest-equippable glider counts as a glider
while it sits in `BODY`, and one so the equip sound still plays.

### What this fixes

Bugs that existed in 2.x and cannot occur in the new design:

- **Picking the elytra up or pressing Q on it** skipped the unequip logic entirely —
  attribute modifiers were left applied, no game event fired, and other players kept
  seeing your wings until the next time you touched the slot.
- **An elytra breaking mid-flight** was never broadcast, so observers saw phantom
  wings and a suppressed cape until the wearer re-equipped something.
- **Joining a server without Trinkets permanently corrupted** a global setting: every
  later singleplayer world in that session silently ran in the wrong mode, and an
  elytra stored in the Trinkets slot became invisible to flight, rendering and
  Mending until the game was restarted.
- **The client and server could disagree about how many slots the inventory had**,
  producing a ghost slot and dropped updates.
- **Glide damage could destroy an elytra at 1 durability.** Vanilla stops using it
  instead; 2.x could break it outright.
- **A failed write to the Trinkets slot destroyed the elytra** — removed from the
  hand, stored nowhere.
- **Dispensers, commands and other non-GUI paths** could put a second elytra on your
  chest while one was already in the slot.
- **Attribute modifiers and enchantment effects were never applied on login or
  respawn**, so a modded elytra lost them until manually re-equipped.

Fixed during this rewrite's own testing:

- The elytra panel was drawn **behind the recipe book**, leaving an invisible but
  still-clickable slot; a stray click could silently unequip your elytra. The panel
  now moves to the other side of the inventory while the book is open.
- Clicking just outside the slot **threw the item on the ground**. A near-miss now
  does nothing, the same as missing a vanilla armour slot.

### Modded elytras

Any item that can glide now works in every slot. The mod keys off the `glider` data
component instead of the vanilla elytra item, so modded elytras equip, fly, take
durability, render, and obey the one-elytra rule exactly like the vanilla one —
including keeping chest-scoped attribute modifiers while worn in the slot.

### Durability displays and gravestone mods

Both work with no compatibility code, because the elytra is real equipment:

- Mods that read live equipment durability see the slot and its damage.
- Gravestone and death-chest mods collect and restore the elytra along with the rest
  of the inventory. 2.x needed per-mod integration and only ever shipped it for one
  grave mod, on one loader.

### Trinkets and Curios

**2.x** integrated with Trinkets only, and layered its own flight and Mending logic
on top of Trinkets' — doing the same work twice.

**3.0.0** has one small `ElytraHost` interface with three implementations, and each
declares what the host mod already does natively:

- **Built-in** (default) — the vanilla `BODY` slot described above.
- **Trinkets** — Trinkets implements flight, durability, Mending, rendering,
  right-click equip and death drops itself, so this mod stands down entirely and
  only supplies the slot definition and the right-click behaviour.
- **Curios** — new in 3.0.0, NeoForge only. Curios provides storage, sync, its GUI,
  Mending and death drops, but no flight and no elytra rendering, so the mod supplies
  those. On NeoForge, gliding is granted through the `neoforge:gliding_flight`
  attribute, which is what the game actually reads.

Which one is active is resolved once at startup and announced to each client on join,
so a client can no longer disagree with the server about where the elytra lives. The
config can force a specific provider, or leave it on automatic.

Both integrations now refuse to overwrite another mod's behaviour for its own winged
items, and gliders are confined to the elytra slot rather than leaking into generic
accessory slots.

### Interaction details

Every way of moving an elytra behaves like vanilla armour:

- Right-click in hand equips it, with the elytra equip sound.
- Shift-click moves it into the elytra slot — including the Trinkets and Curios
  slots, and from inside those mods' own screens.
- Shift-click takes it back out again.
- Number-key and offhand swaps, mouse pickup, Q-throw, creative middle-click and the
  creative destroy slot all work.
- Curse of Binding prevents removal; Curse of Vanishing destroys the elytra on death.
- Only one elytra can be worn at a time across the chest slot, the mod's slot and any
  accessory slot — enforced at a single choke point that covers the GUI, dispensers
  and commands alike.

### Appearance

The inventory panel is unchanged: the same 32x32 nine-slice panel sampled from the
vanilla inventory texture, the same slot recess, the same icon. It now also follows
the recipe book, and appears correctly in the creative inventory tab — where 2.x had
a bug that removed one of the hotbar slots and painted a phantom elytra over another.

### Under the hood

- Roughly half the source files of 2.x, with no custom storage, no custom sync
  packets for slot contents, and no hand-copied vanilla methods.
- 16 mixins, most of them a few lines, plus one optional soft mixin for Curios.
- Multiloader as before: a shared `common` module with Fabric and NeoForge loaders.
- Built against NeoForge 26.2.0.70; the shipped dependency range accepts any 26.2
  build, rather than pinning users to whatever version the mod was compiled with.
- Debug logging removed from hot paths; the single access-transformer/access-widener
  pair is no longer duplicated across modules, and each jar ships only the metadata
  its own loader uses.

### Requirements

The mod must be installed on both the client and the server, as it adds a slot to the
inventory. NeoForge enforces this during connection; Fabric servers refuse the
connection with an explanation.
