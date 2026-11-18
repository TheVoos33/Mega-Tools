---
navigation:
  title: Torches and light
  icon: minecraft:torch
  parent: index.md
  position: 60
---

# Torches and light

<ItemImage id="minecraft:torch" scale="2" />

Put torches or soul torches in the torch slot (up to 64).

* **Light**: the area around you is lit while you hold the tool.
* **Place Torch**: right-click a block to place a torch from the slot.

## Light blocks or shader light

* **Light blocks** (default): an invisible light block follows you. It is real light, so every player sees it and mobs don't spawn near you.
* **Shader light**: with an Oculus shader pack or a dynamic lights mod (Sodium Dynamic Lights, LambDynamicLights, RyoamicLights), that mod lights the tool instead and no light blocks are placed for you. The light is smoother, but only drawn on your screen and doesn't stop mob spawning.

Set `heldLight = false` in `config/megatools-common.toml` to switch the light blocks off. Shaders and dynamic lights mods still light the held tool, since they don't need light blocks.

A light block only replaces air or still water and is removed when you move on. While it is there, the spot
isn't air, so something that needs free space there (a growing sapling, some machines) waits until you have moved away.
