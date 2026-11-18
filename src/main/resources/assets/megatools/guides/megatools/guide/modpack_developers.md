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
| megatools-common.toml | Hold delay, menu style, held light |
| megatools-storage-whitelist.json | Extra items allowed in tool storage |
| megatools-tool-aliases.json | Auto-swap across mods |
| megatools-default-rules.json | Default custom rules |

## Tags

| Tag | Type | Use |
|---|---|---|
| megatools:storable_item | item | Items allowed in tool storage |
| megatools:torches | item | Items allowed in the torch slot |
| megatools:mineable/paxel | block | Blocks paxels mine at tool speed |
| megatools:not_mineable | block | Blocks the Mega Tool is not the right tool for |

## Recipe types

| Recipe type | Like |
|---|---|
| megatools:crafting_shaped_carry_over | minecraft:crafting_shaped, keeps the tool's contents |
| megatools:crafting_shapeless_carry_over | minecraft:crafting_shapeless, keeps the tool's contents. With the same tool as the result, it repairs that tool |

## Resource packs

assets/megatools/mega_tool_animation.json with {"poke": false} switches the Mega Tool to vanilla
animations, as the built-in flat textures pack does.
