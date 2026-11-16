---
navigation:
  title: For modpack developers
  icon: minecraft:writable_book
  parent: index.md
  position: 100
---

# For modpack developers

Everything here is done with config files, datapacks or a recipe script. Nothing needs code. The mod's
README has a full example for every file.

## Config files

All files are in the config folder and are created with their defaults on first launch.

| File | What it sets | Read by | Applied |
|---|---|---|---|
| megatools-startup.toml | Tool stats | client and server | on restart |
| megatools-common.toml | Mega Tool hold delay | client | at once |
| megatools-common.toml | Tool menu style (menuStyle: ASK, CLASSIC or ICONS), each player's own choice | client | the next time a menu opens |
| megatools-common.toml | Held tool light on or off (heldLight) | server | at once |
| megatools-storage-whitelist.json | Items that get the tag megatools:storable_item | server, synced with the tags | on /reload |
| megatools-tool-aliases.json | Auto-swap across mods | client | on joining a world |
| megatools-default-rules.json | Default custom rules | client | on joining a world |

The three JSON files skip everything that belongs to a mod that isn't installed, so one file works for
every version of a pack. Each file starts with a short explanation of itself. A mistake in a file never
crashes the game: the file is ignored and the reason is written to the log.

On large servers, the light that follows players holding a tool with torches can be switched off with
heldLight = false in the server's megatools-common.toml. Every move of that light is a light update for
the server and the players nearby.

## Tool stats

megatools-startup.toml has one section per tool: stone_paxel, iron_paxel, diamond_paxel and mega_tool.

| Option | Meaning |
|---|---|
| attackDamage | Total attack damage, as shown in the tooltip (1 to 10,000). Above 2048, the game's own limit for attack damage is raised to this value. |
| attackSpeed | Attacks per second, as shown in the tooltip (0.1 to 20). A sword has 1.6. |
| durability | Maximum durability (1 to 1,000,000). For the Mega Tool, 0 means unbreakable. |
| miningSpeedMultiplier | Speed compared to the vanilla pickaxe of the same tier (0 to 2, default 1.2). Not for the Mega Tool, which always mines instantly. |

The stats are built into the items when the game starts, so the file is not synced: ship the same file to
clients and the server. A player whose stats differ from the server's gets a chat message on joining. If
you give the Mega Tool durability, also add a repair recipe for it.

## Tool storage whitelist

Tool storage only accepts items that have the item tag megatools:storable_item.
megatools-storage-whitelist.json gives that tag to more items:

| Key | Entry | Effect |
|---|---|---|
| tags | item tags, such as c:tools/wrench | Every item that has one of these tags also gets megatools:storable_item. |
| items | mod id, then item ids (or item tags starting with #) | These items get megatools:storable_item. A group is only used if that mod is installed. |

A datapack can take any of these items out again: list them under "remove" in
data/megatools/tags/item/storable_item.json.

To allow a tool, add its id to the group of its mod, or add a new group. Paxels, the Mega Tool and
Enchantment Plates can never be stored.

## Tool aliases

megatools-tool-aliases.json decides which tools are pre-selected for blocks of other mods.

| Key | Entry | Effect |
|---|---|---|
| items | item id, then mod ids | This one tool is also pre-selected for blocks of those mods. |
| mods | mod id, then mod ids | Every tool of the first mod is also pre-selected for blocks of those mods. |
| groups | any name, then mod ids | A mod family: a tool from any mod in the group is pre-selected for blocks of every mod in it. |
| attachments | entity ids | Things another mod attaches to a block, such as logisticsnetworks:logistics_node. While one sits on the block you look at, the tools of its mod are pre-selected first. |

To add an addon to a family, add its mod id to that group. Labels from Transfer Labels are detected
without an entry.

## Default rules

megatools-default-rules.json holds [custom rules](tool_storage.md#custom-rules) that a paxel or Mega
Tool gets by itself, so commonly used tools work without any setup. Under "rules" there is one group per mod id. Each rule
names the stored tool with "tool" and has exactly one of these:

| Key | Value | Matches |
|---|---|---|
| block | a block id, or the item id of a part (such as ae2:me_p2p_tunnel) | only that block or part |
| tag | a block tag | every block with that tag |
| mod | a mod id | every block from that mod |

The file is created with these rules: with EvilCraft installed, a Flint and Steel in tool storage is
pre-selected for Hardened Blood; with Applied Energistics 2 installed, a Memory Card is pre-selected
for every P2P tunnel, before the wrench.

* A default rule is copied onto a tool the first time the named tool is in its tool storage while the
  player holds it.
* From then on it is a normal filter. Players can change or remove it in the Custom Rules menu.
* Each tool remembers which default rules it already received. A removed rule does not come back on
  that tool, and a rule added to the file later is still copied.

The file is read by each player's game and is never sent by a server, so every player can set the
defaults they like. A modpack ships its defaults by putting the file in the pack's config folder.

## Tags

Add to these with a datapack, with "replace" set to false. A "remove" list takes items out again.

| Tag | Type | Use it to |
|---|---|---|
| megatools:storable_item | item | The only items allowed in tool storage. Already includes the common wrench and shears tags, screwdrivers and crowbars, plus everything megatools-storage-whitelist.json adds. |
| megatools:torches | item | Allow other torches in the torch slot. |
| megatools:mineable/paxel | block | Decide what paxels mine at tool speed. Contains the vanilla pickaxe, axe, shovel and hoe tags. |
| megatools:mega_tool_unbreakable | block | Stop the Mega Tool from breaking a block at all. Empty by default. |

Items in the tag c:tools/wrench can always be stored. The paxels and the Mega Tool are deliberately not
in the vanilla tool tags such as minecraft:pickaxes, because their enchantments must only come from
[Enchantment Plates](enchantment_plates.md).

## Recipes

All recipes are normal datapack recipes in data/megatools/recipe and can be replaced or removed like any
other. The default recipes build each tool from a head, so no tool is ever an ingredient.

If you change that, for example so that an Iron Paxel is upgraded into a Diamond Paxel, use a
**carry-over** recipe type. They are written exactly like the vanilla shaped and shapeless crafting
recipes, but everything in the ingredient tool moves to the result.

| Recipe type | Same format as |
|---|---|
| megatools:crafting_shaped_carry_over | minecraft:crafting_shaped |
| megatools:crafting_shapeless_carry_over | minecraft:crafting_shapeless |
| megatools:tool_repair | minecraft:crafting_shaped |

* **Carried over**: name, ability settings, stored tools, custom rules, torches, the Support Tool and
  both Enchantment Plates. Durability is not: the result is a fresh tool.
* **Nothing is ever deleted.** If the result can't hold everything, for example fewer storage slots than
  stored tools, the recipe doesn't match until the player empties the tool.
* The result has to be a paxel or the Mega Tool.
* megatools:tool_repair returns the damaged tool from the grid itself, fully repaired, with everything on
  it untouched. Use it to change the repair materials.
* With any other recipe type, a tool used as an ingredient is consumed, and everything stored in it is
  given back to the player.
