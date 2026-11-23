# Changelog

## v1.3

Clients and servers must both run v1.3.

### Added
- **Mega Tool Core**: crafted from a Nether Star, a Dragon Head and a Sculk Shrieker down the middle column (top to bottom) with Dragon's Breath in the four corners.
- **Netherite Paxel** and **Netherite Head.** The Netherite Head is crafted from the five netherite tools (shapeless, like the Diamond Head); the Netherite Paxel from the Netherite Head, a chest and leather. The Netherite Paxel has 27 tool storage slots, the durability of the five netherite tools combined (10,155), mines 20% faster than netherite tools, does 22 attack damage with attack speed 3.0 and doesn't burn. Repaired with netherite ingots like the other paxels. 3D model and flat texture.
- **Allthemodium Mega Tool upgrades** (only when the Allthemodium mod is installed; without it they don't exist): **Mega Tool – Allthemodium** (38 attack damage), **Mega Tool – Vibranium** (48) and **Mega Tool – Unobtainium** (60), all with attack speed 3.5. Each is made in a smithing table from the tier before it with the matching upgrade smithing template and ingot (Mega Tool → Allthemodium → Vibranium → Unobtainium). The upgrade keeps everything: stored tools, plates, torches, the Support Tool, settings and custom rules. Otherwise they work exactly like the Mega Tool.
  - With Allthemodium installed, the regular Mega Tool can't break vibranium or unobtainium ore, the Allthemodium Mega Tool can't break unobtainium ore, and the Vibranium and Unobtainium Mega Tools break every block. New block tags `megatools:needs_allthemodium_mega_tool` and `megatools:needs_vibranium_mega_tool` decide which blocks; datapacks can add to them.
  - The in-game guide (GuideME) has an **Allthemodium upgrades** page, shown only when Allthemodium is installed, with attack damage, what each tier breaks and the upgrade recipes. The recipes are read from the game, so recipes changed by a modpack show as changed. Hold the guide key on any of the three tools to open it.
- **Save the file** button in the Custom Rules menu (right of *Inventory*). It writes all custom rules of the tool to `config/megatools-saved-rules.json`, in the format of `megatools-default-rules.json`. Rename it to `megatools-default-rules.json` to give those rules to every new tool, also in another modpack. The chat message after saving links to the file.
- **Mega Tool animation** option (`megaToolAnimation` in `config/megatools-common.toml`, one button under Mods > Mega Tools > Config): **Poke** or **Vanilla**, for every Mega Tool, with the 3D model and with the flat textures.
- Torches you pick up go straight into the torch slot of a paxel or Mega Tool that already holds that kind of torch (main hand first, then off hand, then the inventory). What doesn't fit, or any torch while the slot is empty, goes to the inventory as before.
- **Ability keys**: every switch in the tool menu (Strip, Path, Till, Shear, Brush, Ignite, Delayed Break, Place Torch, Light, Break, Attack) has its own key binding under *Options → Controls → Key Binds → Mega Tools*, named *Toggle Strip* and so on. They are unbound by default. A key only works while a paxel or Mega Tool is in either hand (main hand first); it switches that ability on the held tool and shows the new state above the hotbar.
- **Chat commands for your own settings**, no operator rights or cheats needed, also on a server: `/megatools animation true|false` (poke animation), `/megatools delayed_break 1-200` (hold delay in ticks), and `/megatools menu classic|compact`. They change `config/megatools-common.toml` right away; without a value they show the current setting. On 1.20.1, which has no config screen, this is the in-game way to change them.
- `/megatools held_light true|false` for operators (or with cheats enabled), like `/megatools kit`: switches the held tool light on or off for every player and saves it in the server's `megatools-common.toml`, without a restart. Without a value it shows the current setting.
- Ignite (Support Tool) lights modded TNT too, such as Applied Energistics 2's Tiny TNT: every block that fire can set off.

### Changed
- **The Mega Tool is now an upgrade of the Netherite Paxel** in a smithing table: Netherite Paxel + Mega Tool Core, no template. Everything in the paxel moves over (stored tools, plates, torches, Support Tool, settings, custom rules, name); like every upgrade, the result is a fresh tool. The recipe keeps the ID `megatools:mega_tool`. The guide shows the Mega Tool Core and Mega Tool recipes as read from the game, so recipe changes by a modpack show up there.
- The config menu no longer shows *File format (don't change)* under Tool Stats, so it can't be changed by accident. It stays in `megatools-startup.toml` and still updates the file after Mega Tools updates.
- The torch slot accepts **any torch**: redstone torches and torches from other mods too, not only torches and soul torches. Modded torches no longer need to be added to the `megatools:torches` tag.
- The Mega Tool has **4 rows of tool storage (36 slots)** instead of 3, **28 attack damage** instead of 22 and **attack speed 3.5** instead of 3.0. In `megatools-startup.toml`, values that are still the old defaults are raised to the new ones once; any other value is kept.
- With **Break** switched off, a tool still breaks blocks that an empty hand breaks in one hit, such as torches, crops, sugar cane and flowers.
- The in-game guide and README show the Netherite Paxel everywhere the other paxels are listed, with the Netherite Head, Netherite Paxel and repair recipes.
- Every recipe in the in-game guide is now read from the game instead of a picture: paxels and heads, repair, Support Tool and Enchantment Plate, shown side by side like before. Hovering an item shows its tooltip (and its ID with F3+H), clicking it works like elsewhere in GuideME, the flat textures show when that pack is on, and recipes changed by a modpack show as changed.
- The poke animation now also works with the flat textures pack (before, the pack switched the Mega Tool to the vanilla animations).

### Removed
- **Mega Tool Head** (`megatools:mega_tool_head`) and its recipe: the Mega Tool no longer needs it. Mega Tool Heads in existing worlds disappear when the world is loaded with v1.3.
- `assets/megatools/mega_tool_animation.json`: resource packs no longer switch the Mega Tool animation. Use the `megaToolAnimation` config option instead.

## v1.2

Clients and servers must both run v1.2.

### Added
- **Blacklist filters in Custom Rules.** A **Whitelist / Blacklist** button above the filter slots switches between two lists per stored tool, 18 filters each.
  - Blacklist filters (this block, a tag or the whole mod, like whitelist filters) keep that tool from being pre-selected for their blocks, also when it would be pre-selected for being from the block's mod. The slots are red while the blacklist is shown.
  - A more exact whitelist filter still wins (this block before tag before whole mod; at the same level the whitelist wins). Example: a blacklist filter for all of Applied Energistics 2 plus whitelist filters for the P2P tunnels pre-selects the Memory Card for P2P tunnels and for no other AE2 block.
  - In `megatools-default-rules.json`, `"blacklist": true` makes a default rule a blacklist filter.
- **Flat textures resource pack.** A built-in resource pack, off by default. Enable **Mega Tools Flat Textures** under Options > Resource Packs to replace the 3D models of the paxels, the Mega Tool and the heads with flat 16×16 textures in the vanilla style. While it is on, the Mega Tool uses the vanilla tool animations instead of its poke animation, and the pictures in the in-game guide show the flat textures.
- Resource packs can switch the Mega Tool's poke animation off with `assets/megatools/mega_tool_animation.json` containing `{"poke": false}`.
- Block tag `megatools:not_mineable`. The Mega Tool is the right tool for every block except the blocks in this tag, which it mines like an empty hand (hand speed, only the drops a hand gets). Empty by default.
- The guide's Torches and light page explains the difference between the light blocks and the light from shader packs and dynamic lights mods, and how to switch the light blocks off with `heldLight`.

### Changed
- Place Torch now comes before Strip, Path and Till, so with Place Torch on, right-clicking logs, grass or dirt places a torch.
- The messages shown when a tool's Break or Attack is switched off now read "Breaking is off" and "Attacking is off".
- The in-game guide is much shorter: each page states the features briefly.
  - The tool menu page shows the Classic and Compact menus side by side, with the same number on the same part in both.
  - The Custom Rules picture shows the Whitelist / Blacklist button and the red blacklist slots.
  - The settings page shows the 3D models next to the flat textures of the Mega Tools Flat Textures pack.
- The README follows the order of the in-game guide, with new pictures for repair, the tool menus and the flat textures.
- The Netherite Head is now the **Mega Tool Head**, with the item ID `megatools:mega_tool_head` and a new flat texture. Netherite Heads in existing worlds are lost: craft them again, or finish them into a Mega Tool before updating.
- Paxel repair recipes are now shapeless, and each cobblestone, iron ingot or diamond repairs 1/8 of the paxel. A craft takes one from every slot that has some: 8 around the paxel repair it fully, 2 slots repair 1/4, and a stack in one slot repairs 1/8 per craft, so a paxel can be repaired in the 2x2 inventory grid. Material the paxel doesn't need stays in the grid. The repaired paxel still keeps everything on it.

### Fixed
- Recipes of the types `megatools:crafting_shaped_carry_over` and `megatools:crafting_shapeless_carry_over` crashed the game at startup.
- A newer version of a dynamic lights mod (Sodium Dynamic Lights, LambDynamicLights, RyoamicLights) can no longer crash the game through the held-light hook.
- A tag in `megatools-storage-whitelist.json` that itself contains `#megatools:storable_item` is skipped with a warning. Before, Minecraft threw out both tags and no item could be stored.
- Less work every tick while you look at a block with an auto-swap tool.
- With PickleTweaks installed, its crafting grid repair no longer repairs Mega Tools tools (it fully repaired a paxel with 2 ingots). They always use Mega Tools' own repair recipe.

### Removed
- Recipe type `megatools:tool_repair`. A `megatools:crafting_shapeless_carry_over` recipe whose result is the same item as the tool in the grid now repairs that tool; the default repair recipes use it. Datapacks with `megatools:tool_repair` recipes need to switch to it.
- Block tag `megatools:mega_tool_unbreakable`. Use `megatools:not_mineable` instead; blocks that a datapack still puts in the old tag are treated as `not_mineable` (mined like with an empty hand).

## v1.1 for Minecraft 26.1.2 (NeoForge)

First release of Mega Tools for Minecraft 26.1.2, on NeoForge 26.1.2.126 or newer. It has the same features as Mega Tools v1.1 for 1.21.1. Clients and servers must both run v1.1.

### Differences from the 1.21.1 version
- No EMI support yet, because EMI has no release for 26.1.2. JEI is supported.

### What's new in v1.1 (compared with v1.0 for 1.21.1)

#### Added
- **Break** and **Attack** buttons in the tool menu of every paxel and the Mega Tool, both on by default.
  - Break off: the tool breaks no blocks, in any game mode. Left-click actions on blocks (such as taking items out of drawers) still work in survival.
  - Attack off: the tool does not hit mobs or players, so it does no damage. Item frames, paintings, boats, minecarts and armor stands can still be hit.
- **Compact tool menu.** A second menu style, as wide as a chest, where every button is an item icon (green frame = on, red frame and struck through = off) and all special slots sit together on the right. The first time you open a tool menu you choose between Classic and Compact; change it later under Mods > Mega Tools > Config > Tool menu style (`menuStyle` in `megatools-common.toml`).
- **Auto-swap for attachments.** When another mod has attached something to a block, that mod's stored tools are pre-selected first, then the tools for the block itself. Also works on vanilla blocks.
  - Logistics Network: a Logistics Node on the block you look at.
  - Transfer Labels: a label, while you look at the label itself.
  - More entity types can be listed under `attachments` in `megatools-tool-aliases.json`.
- **Custom rules for parts.** A filter can hold the item of a part (an AE2 P2P tunnel, a cable, a bus) and matches while you look at that part.
- **Default rule:** the AE2 Memory Card is pre-selected for all P2P tunnels, before the wrench.
- **Dynamic lights mods.** With Sodium Dynamic Lights, LambDynamicLights or RyoamicLights installed, that mod lights a held tool with torches, and Mega Tools no longer places its invisible light blocks for that player.
- **New default storable tools:** Logistics Network wrench, Transfer Labels Label Accessor, Machine Settings Copier (Industrial Foregoing, Just Dire Things), Mekanism Configuration Card, Hostile Neural Networks Fab Directive, Crystalix Wand, and the chisels of Rechiseled, Chisel Reborn and Chipped.

#### Changed
- **Tool storage is tag based.** It accepts exactly the items that have the item tag `megatools:storable_item`. `megatools-storage-whitelist.json` no longer allows items directly: it adds that tag to the items it lists.
- Datapacks have the last word over `megatools-storage-whitelist.json`: a `"remove"` list in a `megatools:storable_item` tag file takes out items the config file added.
- `megatools-storage-whitelist.json` has a new `tags` list (default `c:tools/wrench`): every item with one of those tags also gets `megatools:storable_item`.
- `attackDamage` in `megatools-startup.toml` can be set up to 10,000 (was 1,000). Above 2048, the game's attack damage limit is raised to match.
- The tool menu and the Custom Rules menu are drawn from the game's own container texture, so resource packs that restyle the GUIs (dark themes) apply to them.
- The tool menu is one row taller (the Break / Attack row).
- Shift + scroll through pre-selected tools goes the other way (wheel up = next).

#### Fixed
- The Mega Tool's instant mining and hold delay no longer break when another mod removes the flying mining penalty (Draconic Evolution's mining stabilizer, Advanced AE armor, Apothic's Stable Footing and similar).
