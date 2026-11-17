# Changelog

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
