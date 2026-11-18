<p align="center">
  <img src="https://i.imgur.com/r65vFPj.png" alt="Mega Tools" width="100%">
</p>

<p align="center">
  <b>Paxels and the Mega Tool</b>: a pickaxe, axe, shovel, hoe and sword in one item, with built-in storage for your
  other tools, swappable enchantments and a torch slot that lights your way.<br>
  <sub>Minecraft 26.1.2 · NeoForge</sub>
</p>

---

<p align="center">
  <img src="https://i.imgur.com/0degbIM.png" width="64" alt="Stone Paxel">
  <img src="https://i.imgur.com/5805mqO.png" width="64" alt="Iron Paxel">
  <img src="https://i.imgur.com/66WvTai.png" width="64" alt="Diamond Paxel">
  <img src="https://i.imgur.com/wxQYga8.png" width="64" alt="Mega Tool">
</p>

- **[Paxels](#paxels)**: pickaxe, axe, shovel, hoe and sword in one item.
- **[Mega Tool](#the-mega-tool)**: breaks any block instantly and never wears out.
- Every tool has a [menu](#the-tool-menu) with ability toggles, a torch slot, two Enchantment Plates and, from iron up, storage for your other tools.

Hold a tool and press **Ctrl + R** to open its menu.

With GuideME installed, Mega Tools has an in-game guide with the same pages as this README. Open it with the **?** button in a tool menu, or craft it from a book and an iron ingot.

## Pages

- [Paxels](#paxels)
- [The Mega Tool](#the-mega-tool)
- [The tool menu](#the-tool-menu)
- [Support Tool](#support-tool)
- [Enchantment Plates](#enchantment-plates)
- [Torches and light](#torches-and-light)
- [Tool storage and auto-swap](#tool-storage-and-auto-swap)
- [Durability and repair](#durability-and-repair)
- [Controls and settings](#controls-and-settings)
- [For modpack developers](#for-modpack-developers)

---

## Paxels

<p align="center">
  <img src="https://i.imgur.com/0degbIM.png" width="64" alt="Stone Paxel">
  <img src="https://i.imgur.com/5805mqO.png" width="64" alt="Iron Paxel">
  <img src="https://i.imgur.com/66WvTai.png" width="64" alt="Diamond Paxel">
</p>

- Mines everything a pickaxe, axe, shovel and hoe can, **20% faster** than a pickaxe of the same material. Blocks that need a better tier still drop nothing, just like with vanilla tools.
- Cuts cobwebs and fights like a sword, with sweeping attacks.

| | Stone Paxel | Iron Paxel | Diamond Paxel |
|---|:-:|:-:|:-:|
| **Attack damage** | 10 | 14 | 18 |
| **Attack speed** | 1.6 | 2.0 | 2.5 |
| **Durability** | 655 | 1,616 | 8,171 |
| **Tool storage** | – | 9 slots | 18 slots |

Every paxel also has a **Support Tool slot**, **two Enchantment Plate slots** and a **torch slot** (up to 256 torches).

### Right-click abilities

Right-click a block to use them. Each one is switched on or off in the [tool menu](#the-tool-menu).

| Ability | What it does |
|---|---|
| **Place Torch** | Places a torch from the tool's torch slot. *Off by default.* |
| **Strip** | Strips logs and wood, scrapes and removes wax from copper. |
| **Path** | Turns grass and dirt into paths, puts out campfires. |
| **Till** | Turns grass and dirt into farmland. Sneak while right-clicking to till instead of making a path. |
| **Shear / Brush / Ignite** | Shears, brush and flint and steel abilities. Need a [Support Tool](#support-tool). |

If more than one ability fits a block, the first one in the table is used, so a switched-on Place Torch comes before Strip, Path and Till. Brushing suspicious blocks and lighting TNT always come first.

### Break and Attack

Every paxel and the Mega Tool has two more buttons in the [tool menu](#the-tool-menu), both on by default:

- **Break.** Switch it off and the tool breaks no blocks at all, in any game mode. In survival, left-click actions on blocks (such as taking items out of drawers) still work.
- **Attack.** Switch it off and the tool does not hit mobs or players, so it does no damage. Things you hit to pick up or remove (item frames, paintings, boats, minecarts, armor stands) can still be hit.

Right-click abilities, tool storage and auto-swap keep working either way, so a tool can be made safe to carry around a base or to use only as a wrench holder.

### Crafting

Recipes marked *shapeless* can be placed in any arrangement.

<table>
  <tr>
    <td align="center" colspan="2"><img src="https://i.imgur.com/3cyEBNd.png" width="380"><br><b>Stone Paxel</b> <i>(shapeless)</i><br>Stone sword, axe, pickaxe, shovel and hoe</td>
  </tr>
  <tr>
    <td align="center"><img src="https://i.imgur.com/HEipIYD.png" width="380"><br><b>Iron Head</b> <i>(shapeless)</i><br>Iron sword, axe, pickaxe, shovel and hoe</td>
    <td align="center"><img src="https://i.imgur.com/46UpBXU.png" width="380"><br><b>Iron Paxel</b><br>Iron Head, chest and leather</td>
  </tr>
  <tr>
    <td align="center"><img src="https://i.imgur.com/BYwzvjK.png" width="380"><br><b>Diamond Head</b> <i>(shapeless)</i><br>Diamond sword, axe, pickaxe, shovel and hoe</td>
    <td align="center"><img src="https://i.imgur.com/fnwJ0zZ.png" width="380"><br><b>Diamond Paxel</b><br>Diamond Head, chest and leather</td>
  </tr>
</table>

---

## The Mega Tool

<img src="https://i.imgur.com/wxQYga8.png" width="96" align="right">

- **Instant mining.** Breaks every block in one hit, and counts as the right tool for every block, so everything drops. Unbreakable blocks such as bedrock stay unbreakable. Blocks in the `megatools:not_mineable` tag are the exception: there the Mega Tool works like an empty hand. Flying, swimming and other mods' mining speed changes make no difference.
- **Delayed Break.** While you **hold** left click, the Mega Tool pauses briefly between blocks so you don't dig further than you meant to. Every new click still breaks a block immediately. Switch it off in the [tool menu](#the-tool-menu) to break as fast as you can aim. The pause length can be changed in the [settings](#settings).
- **Never wears out**, and it doesn't burn in fire or lava.
- **Attack damage 22**, attack speed 3.0 and **27 slots** of [tool storage](#tool-storage-and-auto-swap).
- Has every [paxel ability](#right-click-abilities).

### Crafting

<table>
  <tr>
    <td align="center"><img src="https://i.imgur.com/cCWOWso.png" width="380"><br><b>Mega Tool Head</b> <i>(shapeless)</i><br>Netherite sword, axe, pickaxe, shovel, hoe and a Nether Star</td>
    <td align="center"><img src="https://i.imgur.com/oQdx8MC.png" width="380"><br><b>Mega Tool</b><br>Mega Tool Head, chest and leather</td>
  </tr>
</table>

---

## The tool menu

Hold a paxel or the Mega Tool and press **Ctrl + R**.

<p align="center">
  <img src="https://i.imgur.com/NEjZVFr.png" width="100%" alt="The Classic (left) and Compact (right) tool menus of the Mega Tool">
</p>

There are two menu styles, **Classic** (left) and **Compact** (right). Both do exactly the same, and each number marks the same part in both menus.

| | |
|:-:|---|
| **1** | **Name.** Click it to rename the tool, then press Enter. |
| **2** | **Ability buttons.** Green ✔ = on, red ✖ = off. Row 1: Break and Attack (see [Break and Attack](#break-and-attack)). Row 2: Strip, Path, Till. Row 3: Shear, Brush, Ignite (only shown while a Support Tool is in its slot). Last row: Delayed Break (Mega Tool only) and Place Torch. |
| **3** | **Support Tool slot.** Unlocks Shear, Brush and Ignite. |
| **4** | **Torch slot** (up to 256 torches). The small button below it switches the light on or off. |
| **5** | **Enchantment Plates.** Primary (top) and secondary (bottom). The gold frame marks the loaded plate. |
| **6** | **Tool storage.** Hover a stored tool and press **R** to close the menu and swap straight to it. |
| **7** | **Custom Rule.** Opens the [Custom Rules menu](#custom-rules) of this tool, where you choose which blocks pre-select which stored tool. Only on tools with tool storage. |

Shift-clicking an item in your inventory moves it to the right slot automatically. The tool stays in your hotbar while the menu is open, and its slot is locked.

### Classic or Compact

The first time you open a tool menu you are shown both styles and pick one:

- **Classic**: a wide menu, every button with its name on it.
- **Compact**: as wide as a chest, every button an item icon. A green frame means on; a red frame with the icon dimmed and struck through means off. Hover a button to read its name and what it does. Shear, Brush and Ignite are greyed out until a Support Tool is in its slot.

In the Compact menu the first row is Break, Attack, Delayed Break (Mega Tool only), Strip, Path and Till; the second row is Place Torch, Shear, Brush and Ignite. The four slots on the right are the two Enchantment Plates (top), the torch slot with its light button (bottom left) and the Support Tool slot (bottom right).

Your choice is saved for your game. Change it at any time under **Mods > Mega Tools > Config > Tool menu style**, or with `menuStyle` in `config/megatools-common.toml` (`CLASSIC`, `ICONS`, or `ASK` to be asked again).

---

## Support Tool

<img src="https://i.imgur.com/7a9ErY0.png" width="80" align="right">

Craft a Support Tool from a flint and steel, a brush and shears, and put it in a tool's **Support Tool slot**. The tool then gains three abilities, each with its own button in the menu:

- **Shear:** shear sheep, mooshrooms and snow golems; collect leaves and vines; trim cave vines, kelp and weeping or twisting vines so they stop growing. Leaves and wool also mine at shears speed.
- **Brush:** hold right click on suspicious sand or gravel.
- **Ignite:** light fires and nether portals, campfires, candles and TNT.

Taking the Support Tool out removes the abilities again. It works in every paxel and in the Mega Tool.

### Crafting

<table>
  <tr>
    <td align="center"><img src="https://i.imgur.com/3nVOqGb.png" width="380"><br><b>Support Tool</b> <i>(shapeless)</i><br>Flint and steel, brush and shears</td>
  </tr>
</table>

---

## Enchantment Plates

<img src="https://i.imgur.com/GZwuQWB.png" width="80" align="right">

The tools themselves can't be enchanted. You enchant **plates** instead, and the plate loaded in a tool gives it its enchantments.

**Enchanting a plate**
- **Enchanting table:** put in a blank plate, just like a book.
- **Anvil:** a plate on the left, an enchanted book on the right. Matching levels combine (Efficiency IV + IV = V).

Plates only accept enchantments for swords, pickaxes, axes, shovels, hoes, shears, brushes and flint and steel. Enchantments that don't go together, such as Silk Touch and Fortune, can't be put on the same plate.

**Two plates per tool.** Every tool holds a primary and a secondary plate. Press **Alt + R** to switch between them, for example from a Silk Touch plate to a Fortune plate. The message above the hotbar shows which plate was loaded. Rename a plate in an anvil to see that name in the message.

> [!TIP]
> If something removes an enchantment from your tool, such as a disenchanting machine from another mod, the loaded plate loses it too.

### Crafting

<table>
  <tr>
    <td align="center"><img src="https://i.imgur.com/cgFOUbx.png" width="380"><br><b>Enchantment Plate</b><br>A book surrounded by 8 iron ingots</td>
  </tr>
</table>

---

## Torches and light

Put **torches** or **soul torches** in the torch slot (up to 256).

- **Light.** While you hold the tool in either hand, the area around you is lit like a torch at full brightness. Use the small button under the torch slot to switch it off.
- **Place Torch.** Switch it on in the menu, then right-click a block to place a torch from the slot.

### Light blocks or shader light

- **Light blocks** (default): an invisible light block follows you. It is real light, so every player sees it and mobs don't spawn near you.
- **Shader light**: with an Iris shader pack that supports handheld light, or a dynamic lights mod (Sodium Dynamic Lights, LambDynamicLights, RyoamicLights), that mod lights the held tool instead and no light blocks are placed for you. The light is smoother and follows that mod's own settings, but it is only drawn on your screen and doesn't stop mob spawning. Other players only see it if they have such a mod too.

Switch the light blocks off with **Held tool light** under *Mods → Mega Tools → Config*, or `heldLight = false` in `config/megatools-common.toml`. Shaders and dynamic lights mods still light the held tool, since they don't need light blocks.

A light block only ever takes the place of air or still water, and it is removed when you move on. While it is there, that spot isn't air: something that needs free space there (a sapling or a crop growing, a machine that checks for air) waits until you have moved away. It is placed without a block place event, so claim and protection mods don't stop it.

---

## Tool storage and auto-swap

The Iron Paxel, Diamond Paxel and Mega Tool can store **9, 18 and 27** other tools: wrenches, shears, screwdrivers, crowbars, configurators, building gadgets, construction sticks and more. Each slot holds one tool. Only items known to be tools are accepted, so the storage can't be used as a backpack. Modpacks can add more (see [which items can be stored](#which-items-can-be-stored)). Paxels, the Mega Tool and Enchantment Plates can never be stored.

> [!NOTE]
> If a crafting recipe ever uses up one of these tools, everything stored in it (tools, plates, Support Tool and torches) is given back to you.

### Auto-swap

<p align="center">
  <img src="https://i.imgur.com/95QX0x6.png" width="640" alt="Pre-selected tools next to the hotbar">
</p>

1. Look at a block from another mod. Stored tools from the **same mod** are pre-selected and shown next to your hotbar.
2. Press **R** to swap the paxel for the selected tool. Your paxel is kept safely inside it.
3. Press **R** again to swap back. The tool returns to its storage slot.

**Swap to the last tool.** Press **Shift + R** to swap to the tool you swapped to last time, no matter what block you are looking at. Press **R** or **Shift + R** to swap back to your paxel. The tool is found even if you moved it to another storage slot.

If several stored tools match, **Shift + scroll** picks between them. Vanilla blocks never pre-select a tool on their own, because the paxel already handles them. If a swapped-out tool breaks, your paxel is given back to you automatically.

### Custom rules

Auto-swap only knows that a tool belongs to the blocks of its own mod. Custom rules let you decide yourself which stored tool is pre-selected for which block: a wrench for every chest, shears for leaves, a Flint and Steel for one special block. They also work on vanilla blocks.

Open the [tool menu](#the-tool-menu) and click **Custom Rule** above the tool storage.

<p align="center">
  <img src="https://i.imgur.com/gGYzSBW.png" width="560" alt="Custom Rules menu of the Mega Tool">
</p>

| | |
|:-:|---|
| **1** | **Back.** Returns to the tool menu. |
| **2** | **Stored tools.** One cell for every kind of tool in the tool storage. Click a tool to select it (gold frame). The small number is how many filters it has. |
| **3** | **Filters** of the selected tool: up to 18 blocks. The **Whitelist / Blacklist** button above the filter slots switches between the tool's two lists (see [Blacklist](#custom-rules) below). |
| **4** | **Copy** and **Paste.** Move all rules from one paxel or Mega Tool to another. |

**Setting up a rule**

1. **Select the stored tool** the rule is for (**2**).
2. **Give it a block.** Any of these fills the next free filter slot (**3**):
   - pick the block up from your inventory and click a filter slot,
   - Shift-click the block in your inventory,
   - drag the block from **JEI** onto a filter slot.

   Nothing is used up: the slot only remembers the block, and the item stays where it was.
3. **Choose what the filter matches.** Left-click a filled slot to go to the next option:

   | Option | Matches | Shown as |
   |---|---|---|
   | This block | only that block | plain slot |
   | Tag | every block that has the chosen block tag, for example `#c:chests` | purple frame and **#** |
   | Whole mod | every block from the block's mod | blue frame and **@** |

   On a tag filter, **scroll** over the slot to pick which of the block's tags is used. The tooltip lists all of them and marks the chosen one.
4. **Right-click** a filter to remove it. Clicking a filled slot while holding another block replaces it.

In the picture, the wrench is pre-selected for pistons (this block), for every barrel (tag) and for every block from the furnace's mod (whole mod).

**Blacklist.** Every stored tool has a second list of 18 filters. Click **Whitelist** above the filter slots to switch to the **Blacklist** (the button turns red, and so do the slots); click it again to go back. Blacklist filters are set up exactly like the others (this block, tag or whole mod), but do the opposite: the tool is **not** pre-selected for those blocks, not even because it is from the block's mod.

A whitelist filter still works when it is more exact than the blacklist filter that matches the same block (this block before tag before whole mod; at the same level the whitelist wins). Example: give the Memory Card a blacklist filter for the whole of Applied Energistics 2 and keep its whitelist filters for the P2P tunnels. It is then pre-selected for P2P tunnels and for no other AE2 block, so the wrench comes first everywhere else.

**Using it.** Close the menu and look at a matching block with the paxel or Mega Tool in your hand. The stored tool appears next to your hotbar: press **R** to swap to it.

**Which tool comes first.** If several tools match a block, they are offered in this order, and **Shift + scroll** picks between them:

1. filters for exactly this block,
2. tag filters,
3. whole-mod filters,
4. the automatic same-mod tools.

A tool that a blacklist filter matches is left out of this list (unless a more exact whitelist filter matches too).

**Copying rules to another tool.** Custom rules belong to **one** paxel or Mega Tool. To bring them over, click **Copy**, open the Custom Rules menu of the other tool and click **Paste**. Paste replaces all rules of that tool. A default rule you removed on the first tool is not added again on the second. The copy is kept until you close the game. Rules name the stored tool by its kind, so they work as soon as the same kind of tool is in the other tool's storage, in any slot.

**Good to know**

- A tool that has filters but is no longer in the tool storage is shown faded. Its filters are kept and work again when the tool is put back.
- Some filters are added for you: the [default rules](#default-rules). They appear the first time the tool they belong to is put in the tool storage. They are normal filters: change or remove them as you like. A default rule you removed does not come back on that tool.
- **Parts.** Blocks made of parts (cables with attached parts, such as Applied Energistics 2 P2P tunnels and buses, or conduit bundles) have no block item. Use the **part's item** as the filter: it matches while you look at that part. Example: give the Memory Card a filter for each P2P tunnel, and it is pre-selected before the wrench whenever you look at a P2P tunnel. A filter matches whatever item the pick-block key (middle mouse button) gives for the spot you look at. A tag or whole-mod filter still covers every part.


---

## Durability and repair

Paxels **never break**. At 1 durability a paxel stops working: no mining, fighting or abilities until it is repaired. Its menu, storage and plates still work, so nothing is ever lost.

**8 resources of the tier fully repair a paxel**, and a paxel that isn't fully broken uses fewer. The resources are cobblestone, iron ingots and diamonds.

<table>
  <tr>
    <th></th>
    <th>One resource</th>
    <th>Eight resources</th>
  </tr>
  <tr>
    <td align="center"><b>Stone Paxel</b></td>
    <td align="center"><img src="https://i.imgur.com/ZJlmhWS.png" width="290" alt="Repairing a broken Stone Paxel with one cobblestone"></td>
    <td align="center"><img src="https://i.imgur.com/OknKdKZ.png" width="290" alt="Repairing a broken Stone Paxel with 8 cobblestone"></td>
  </tr>
  <tr>
    <td align="center"><b>Iron Paxel</b></td>
    <td align="center"><img src="https://i.imgur.com/gpw2un4.png" width="290" alt="Repairing a broken Iron Paxel with one iron ingot"></td>
    <td align="center"><img src="https://i.imgur.com/vnqA1Au.png" width="290" alt="Repairing a broken Iron Paxel with 8 iron ingots"></td>
  </tr>
  <tr>
    <td align="center"><b>Diamond Paxel</b></td>
    <td align="center"><img src="https://i.imgur.com/lRj0vXm.png" width="290" alt="Repairing a broken Diamond Paxel with one diamond"></td>
    <td align="center"><img src="https://i.imgur.com/xnnDfGo.png" width="290" alt="Repairing a broken Diamond Paxel with 8 diamonds"></td>
  </tr>
</table>

Each resource repairs **1/8** of the paxel's durability. Like any crafting, a craft takes one from every slot that has some: 8 around the paxel repair it fully, resources in 2 slots repair 1/4, and a stack in one slot repairs 1/8 per craft, so it works in the 2x2 inventory grid too. Resources the paxel doesn't need any more stay in the grid. It keeps its name, storage, plates and everything else. Repairing in an anvil with the same resources works too, as does Mending from a plate.

---

## Controls and settings

| Action | Default key |
|---|---|
| Open the tool menu | **Ctrl + R** |
| Switch the loaded Enchantment Plate | **Alt + R** |
| Swap to the pre-selected tool / swap back | **R** |
| Swap to the last tool you swapped to | **Shift + R** |
| Choose between pre-selected tools | **Shift + mouse wheel** |
| Swap to a stored tool from the menu | Hover it, then **R** |

All keys can be changed under *Options → Controls → Key Binds → Mega Tools*. Tooltips always show your current keys.

### Settings

Open *Mods → Mega Tools → Config* in game.

| Setting | What it changes |
|---|---|
| **Mega Tool hold delay** | Pause between blocks while holding left click with Delayed Break on. Default 10 ticks (half a second). |
| **Held tool light** | Whether a held tool with torches lights the area around you. On a server, the server's setting decides. On by default. |
| **Tool menu style** | Classic or Compact, see [Classic or Compact](#classic-or-compact). Each player's own choice. |
| **Tool Stats** | Attack damage, attack speed, durability and mining speed of each tool. A restart is needed after changing these. |

**Tool Stats need permission.** They can only be changed from inside a world you host (singleplayer or open to LAN) with cheats enabled, or as its operator. On a server, the server's own tool stats are always used: server operators change them in the server's `config/megatools-startup.toml` and restart it. If your game's tool stats differ from the server's, you get a chat message when you join, because durability and mining would otherwise look wrong.

**Operators** can use `/megatools kit` to get every tool, Support Tools, Enchantment Plates, torches and enchanted books for testing.


### 3D models or flat textures

By default, the paxels, the Mega Tool and the heads use **3D models**. To use flat 16×16 textures in the vanilla style instead, enable the built-in resource pack **Mega Tools Flat Textures** under *Options → Resource Packs*. The Mega Tool then uses the vanilla tool animations instead of its poke animation, and the pictures in the in-game guide show the flat textures.

<p align="center">
  <img src="https://i.imgur.com/0dW6G4w.png" width="100%" alt="The 3D models (left) and the flat 2D textures (right)">
</p>

---

## For modpack developers

Everything here is done with config files, datapacks or a recipe script. Nothing needs code.

### Config files

All files are in the `config` folder and are created with their defaults on first launch.

| File | What it sets | Read by | Applied |
|---|---|---|---|
| `megatools-startup.toml` | [Tool stats](#tool-stats) | client **and** server | on restart |
| `megatools-common.toml` | Mega Tool hold delay (`megaToolHoldDelayTicks`) | client | at once |
| `megatools-common.toml` | Tool menu style (`menuStyle`: `ASK`, `CLASSIC` or `ICONS`), each player's own choice | client | the next time a menu opens |
| `megatools-common.toml` | Held tool light on or off (`heldLight`) | server | at once |
| `megatools-storage-whitelist.json` | [Items that get the `megatools:storable_item` tag](#which-items-can-be-stored) | server, synced with the tags | on `/reload` |
| `megatools-tool-aliases.json` | [Auto-swap across mods](#auto-swap-for-tools-from-other-mods) | client | on joining a world |
| `megatools-default-rules.json` | [Default custom rules](#default-rules) | client | on joining a world |

**Large servers:** the held light is an invisible light block that follows each player holding a tool with torches. Every move is a light update for the server and for the players nearby. Set `heldLight = false` in the server's `megatools-common.toml` to switch it off. Players with a shader pack that supports handheld light, or with a dynamic lights mod (Sodium Dynamic Lights, LambDynamicLights, RyoamicLights), still get their own light. For those players no light block is placed in any case.

The three JSON files skip everything that belongs to a mod that isn't installed, so one file works for every version of a pack. A mistake in a file never crashes the game: the file is ignored and the reason is written to the log. Keys starting with `_` (such as `_help`) are notes and are ignored. Press **F3 + H** in game to see item ids in tooltips.

### Tool stats

`config/megatools-startup.toml` has one section per tool: `[stone_paxel]`, `[iron_paxel]`, `[diamond_paxel]` and `[mega_tool]`.

```toml
[iron_paxel]
	attackDamage = 14.0
	attackSpeed = 2.0
	durability = 1616
	miningSpeedMultiplier = 1.2
```

| Option | Meaning | Range |
|---|---|---|
| `attackDamage` | Total attack damage, as shown in the tooltip. Above 2048 (the game's own limit for attack damage) Mega Tools raises that limit to the value set here. | 1 to 10,000 |
| `attackSpeed` | Attacks per second, as shown in the tooltip. 1.6 is a sword, 4.0 has no cooldown. | 0.1 to 20 |
| `durability` | Maximum durability. For the Mega Tool, `0` means unbreakable (the default). | 1 to 1,000,000 (Mega Tool: from 0) |
| `miningSpeedMultiplier` | Mining speed compared to the vanilla pickaxe of the same tier. 1.2 is 20% faster, 0 can't mine. The Mega Tool doesn't have it: it always mines instantly. | 0 to 2 |

The stats are built into the items when the game starts, so the file is not synced: **ship the same file to clients and the server**. A player whose stats differ from the server's gets a chat message on joining. If you give the Mega Tool durability, also add a [repair recipe](#recipes) for it, because it has none by default.

### Which items can be stored

An item fits in tool storage only if it has the **`megatools:storable_item`** item tag. Nothing else is checked.

Items get that tag in two ways:

- from datapacks, like any other tag (see [tags](#tags)),
- from the whitelist file **`config/megatools-storage-whitelist.json`**, which adds the tag to the items and tags it lists. This is the easy way for tools that have no suitable tag.

```json
{
  "version": 2,
  "tags": ["c:tools/wrench", "yourmod:all_tools"],
  "items": {
    "sfm": ["sfm:labelgun", "sfm:network_tool"],
    "constructionstick": ["constructionstick:iron_stick", "constructionstick:diamond_stick"],
    "yourmod": ["yourmod:some_tool"]
  }
}
```

- **`tags`**: every item that has one of these item tags also gets `megatools:storable_item`.
A datapack has the last word: the file's entries go into the tag before the datapacks' entries, so a datapack can take any of them out again with a `"remove"` list (see [tags](#tags)). `"replace": true` only replaces the entries of other datapacks, not those of this file.

- **`items`**: item ids that get the tag, grouped by mod id. A group is only used when that mod is installed. A value starting with `#` is an item tag. To add a tool, add its id to the group of its mod, or add a new group.

Ids and tags that don't exist are skipped. Leave `version` as it is.

The file is created with `c:tools/wrench`, vanilla's Flint and Steel and tools from many popular mods (Super Factory Manager, Construction Sticks, Building and Mining Gadgets, Functional Storage, Sophisticated Storage, PneumaticCraft, Framed Blocks, Mekanism, Industrial Foregoing, Just Dire Things, Logistics Network, Transfer Labels, Rechiseled, Chisel Reborn, Chipped and more). A file from Mega Tools 1.0 is updated once: the `tags` list and the new default tools are added, and your own entries are kept.

Because these are real tag entries, they show up wherever tags do (JEI, `/reload`, other mods), and on a server the server's file decides: tags are synced to every player.

Paxels, the Mega Tool and Enchantment Plates can never be stored, whatever the file says. Tools that were stored before a change stay in storage even if they are no longer accepted.

### Auto-swap for tools from other mods

A stored tool is normally pre-selected only for blocks from its own mod. To use a tool on other mods' blocks as well, add it to `config/megatools-tool-aliases.json`:

```json
{
  "items":  { "modern_industrialization:wrench": ["mekanism", "create"] },
  "mods":   { "integrateddynamics": ["integratedterminals"] },
  "groups": { "create": ["create", "createaddition", "create_connected"] },
  "attachments": ["logisticsnetworks:logistics_node"]
}
```

| Key | Entry | Effect |
|---|---|---|
| `items` | item id → mod ids | This one tool is also pre-selected for blocks of those mods. |
| `mods` | mod id → mod ids | Every tool of the first mod is also pre-selected for blocks of those mods. |
| `groups` | any name → mod ids | A mod family: a tool from any mod in the group is pre-selected for blocks of every mod in it. The name is only a label. |
| `attachments` | entity ids | Things another mod attaches to a block. While one sits on the block you look at, the stored tools of its mod are pre-selected first, then the tools for the block itself. |

A value can be one mod id or a list. To add an addon to a family, add its mod id to that group. The file is created with groups for common families, such as Applied Energistics 2 with its addons (ExtendedAE, AdvancedAE, MEGA Cells…), Create with its addons, Mekanism, Refined Storage, RFTools with XNet, Integrated Dynamics, Ender IO, Modern Industrialization, Ars Nouveau and more. `minecraft` has no effect here: vanilla blocks only pre-select a tool through a custom or default rule.

**Attachments.** Some mods put their own part on a block that isn't theirs. Mega Tools detects two of them, also on vanilla blocks such as chests:

- **Logistics Network**: a Logistics Node on the block you look at pre-selects the stored Logistics Network tools (the wrench), followed by the tools for the block under it.
- **Transfer Labels**: a label between two blocks pre-selects the stored Transfer Labels tools (the Label Accessor) while you look at the label itself, not at the blocks it connects.

Other mods that attach an entity to a block can be added to `attachments`. A file from Mega Tools 1.0 has no `attachments` key and uses the default.

### Default rules

Default rules are [custom rules](#custom-rules) that a paxel or Mega Tool gets by itself, so commonly used tools work without any setup. They are listed in `config/megatools-default-rules.json`:

```json
{
  "rules": {
    "evilcraft": [
      { "tool": "minecraft:flint_and_steel", "block": "evilcraft:hardened_blood" }
    ],
    "minecraft": [
      { "tool": "minecraft:shears", "tag": "#minecraft:leaves" },
      { "tool": "yourmod:wrench", "mod": "othermod" }
    ]
  }
}
```

Rules are grouped by mod id, and a group is only used if that mod is installed (`minecraft` always is). Each rule names the stored `tool` and **exactly one** of:

| Key | Value | Matches |
|---|---|---|
| `block` | a block id, or the item id of a part (such as `ae2:me_p2p_tunnel`) | only that block or part |
| `tag` | a block tag, such as `#c:chests` | every block with that tag |
| `mod` | a mod id | every block from that mod |

Add `"blacklist": true` to a rule to make it a [blacklist](#custom-rules) filter: the tool is then **not** pre-selected for those blocks. For example `{ "tool": "ae2:memory_card", "mod": "ae2", "blacklist": true }`.

The file is created with two sets of rules:

- **EvilCraft**: the rule above. With a Flint and Steel in tool storage, looking at Hardened Blood pre-selects it.
- **Applied Energistics 2**: the Memory Card for every P2P tunnel (ME, item, fluid, FE, redstone, light). With a Memory Card in tool storage, looking at a P2P tunnel pre-selects it before the wrench. P2P tunnels are parts, so `block` holds the part's item id, such as `ae2:me_p2p_tunnel`.

A file from Mega Tools 1.0 is updated once: the new default rules are added and your own are kept. How default rules reach a tool:

- A default rule is copied onto a paxel or Mega Tool the first time the named tool is in its tool storage while you hold it.
- From then on it is a normal filter of that tool. Players can change or remove it in the Custom Rules menu.
- Each tool remembers which default rules it already received. A removed rule does not come back on that tool, and a rule added to the file later is still copied.
- A tag rule shows the first block of its tag in its filter slot, and a mod rule shows a block of that mod.
- If the tool has no free filter slot for that stored tool (18 per stored tool), the rule is not added.

The file is read by each player's game and is never sent by a server, so every player can set the defaults they like. A modpack ships its defaults by putting the file in the pack's config folder.

### Tags

Add to these with a datapack (or a script mod such as KubeJS), using `"replace": false`. For example `data/megatools/tags/item/storable_item.json`:

```json
{
  "replace": false,
  "values": ["yourmod:some_tool", "#yourmod:all_tools"]
}
```

To take an item out of a tag again, list it under `"remove"`. This also works for items that `megatools-storage-whitelist.json` added:

```json
{
  "replace": false,
  "values": [],
  "remove": ["mekanism:configuration_card", "#c:tools/shear"]
}
```

| Tag | Type | Use it to |
|---|---|---|
| `megatools:storable_item` | item | The only items allowed in tool storage. Already includes `c:tools/wrench`, `c:wrenches`, `forge:tools/wrench`, `c:tools/shear` (shears and chainsaws), Immersive Engineering's screwdrivers and Railcraft's crowbars. |
| `megatools:torches` | item | Allow other torches in the torch slot (by default the torch and soul torch). The light is always full brightness, and Place Torch places the item as a block. |
| `megatools:mineable/paxel` | block | Decide what paxels mine at tool speed. Contains the vanilla pickaxe, axe, shovel and hoe `mineable` tags; add blocks that no tool tag covers. |
| `megatools:not_mineable` | block | Blocks the Mega Tool is not the right tool for. It mines them like an empty hand: hand speed and only the drops a hand gets. Empty by default. Replaces `megatools:mega_tool_unbreakable` (removed in v1.2); blocks a datapack still puts in that tag are added to this one. |

Two vanilla tag groups matter as well. Which tier a block needs still follows `minecraft:incorrect_for_stone_tool`, `…_iron_tool` and `…_diamond_tool` (the Mega Tool drops everything). The Enchantment Plate is in the `minecraft:enchantable/…` item tags for swords and mining tools, which is why the enchanting table offers it those enchantments. The paxels and the Mega Tool are deliberately **not** in `minecraft:pickaxes`, `minecraft:swords` and so on, because their enchantments must only come from plates.

### Recipes

All recipes are normal datapack recipes in `data/megatools/recipe/` and can be replaced or removed like any other: `stone_paxel`, `iron_head`, `diamond_head`, `mega_tool_head`, `iron_paxel`, `diamond_paxel`, `mega_tool`, `support_tool`, `enchantment_plate`, and the repair recipes `stone_paxel_repair`, `iron_paxel_repair` and `diamond_paxel_repair`.

The default recipes build each tool from a head, so no tool is ever an ingredient. If you change that, for example so that an Iron Paxel is upgraded into a Diamond Paxel, use one of the **carry-over** recipe types. They are written exactly like `minecraft:crafting_shaped` and `minecraft:crafting_shapeless`, but everything in the ingredient tool moves to the result:

| Recipe type | Same format as |
|---|---|
| `megatools:crafting_shaped_carry_over` | `minecraft:crafting_shaped` |
| `megatools:crafting_shapeless_carry_over` | `minecraft:crafting_shapeless` |

```json
{
  "type": "megatools:crafting_shaped_carry_over",
  "category": "equipment",
  "pattern": [
    " D ",
    "DPD",
    " D "
  ],
  "key": {
    "D": { "item": "minecraft:diamond" },
    "P": { "item": "megatools:iron_paxel" }
  },
  "result": { "id": "megatools:diamond_paxel" }
}
```

- **Carried over:** name, ability settings, stored tools, custom rules, torches, the Support Tool and both Enchantment Plates (the loaded plate is applied to the new tool). Durability is not: the result is a fresh tool.
- **Nothing is ever deleted.** If the result can't hold everything, for example fewer storage slots than stored tools, the recipe doesn't match until the player empties the tool. It also doesn't match if two ingredient tools both hold items.
- The result has to be a paxel or the Mega Tool.
- **Repair:** a `megatools:crafting_shapeless_carry_over` recipe whose result is the same item as the tool in the grid repairs that tool instead. Each material repairs 1/n of the tool's durability (n = the number of materials in the recipe), the tool comes out with everything on it untouched, and the recipe only matches a damaged tool. The default repair recipes work this way (8 materials and the paxel). All materials must be accepted by every material ingredient; any number of slots from 1 to n may hold them, and a craft takes one from each slot it needs. Use it to change the repair materials or to add a repair recipe for a Mega Tool with durability. Anvil repair materials are fixed: stone tool materials, iron ingots and diamonds.
- With a plain `minecraft:` recipe type (or another mod's), a tool used as an ingredient is consumed, and everything stored in it is given back to the player instead of being carried over.
- A stored tool that a player has swapped to carries the paxel inside it. If a recipe uses that tool up, the paxel is given back to the player.


### Resource packs

The flat textures pack switches the Mega Tool to the vanilla animations with `assets/megatools/mega_tool_animation.json`. Other resource packs can do the same:

```json
{ "poke": false }
```

---

<p align="center"><sub>Mega Tools is licensed under the GNU GPLv3.</sub></p>
