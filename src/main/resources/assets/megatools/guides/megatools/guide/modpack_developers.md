---
navigation:
  title: For modpack developers
  icon: minecraft:writable_book
  parent: index.md
  position: 100
---

# For modpack developers

The README has a full example for every file.

## Config files

| File | Sets |
|---|---|
| megatools-startup.toml | Tool stats (restart needed, same file on client and server) |
| megatools-common.toml | Hold delay, menu style, held light, Mega Tool animation |
| megatools-storage-whitelist.json | Extra items allowed in tool storage |
| megatools-tool-aliases.json | Auto-swap across mods |
| megatools-default-rules.json | Default custom rules |

## Tags

| Tag | Type | Use |
|---|---|---|
| megatools:storable_item | item | Items allowed in tool storage |
| megatools:torches | item | Extra items allowed in the torch slot (every torch is allowed already) |
| megatools:mineable/paxel | block | Blocks paxels mine at tool speed |
| megatools:not_mineable | block | Blocks the Mega Tool is not the right tool for |
| megatools:needs_allthemodium_mega_tool | block | Blocks the regular Mega Tool can't break (vibranium and unobtainium ore) |
| megatools:needs_vibranium_mega_tool | block | Blocks the Allthemodium Mega Tool can't break either (unobtainium ore) |

## Recipe types

| Recipe type | Like |
|---|---|
| megatools:crafting_shaped_carry_over | minecraft:crafting_shaped, keeps the tool's contents |
| megatools:crafting_shapeless_carry_over | minecraft:crafting_shapeless, keeps the tool's contents. With the same tool as the result, it repairs that tool |

## Mega Tool animation

The Mega Tool animation is a config option (megaToolAnimation in config/megatools-common.toml, POKE or
VANILLA), no longer a resource pack file. The assets/megatools/mega_tool_animation.json of older versions
is ignored.
