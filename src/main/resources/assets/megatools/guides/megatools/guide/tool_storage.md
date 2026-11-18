---
navigation:
  title: Tool storage and auto-swap
  icon: minecraft:chest
  parent: index.md
  position: 70
---

# Tool storage and auto-swap

The Iron Paxel, Diamond Paxel and Mega Tool store **9, 18 and 27** other tools, such as wrenches,
shears and configurators. Only tools are accepted.

## Auto-swap

![Pre-selected tools next to the hotbar](auto_swap_hud.png)

* Look at a block from another mod: its stored tools are shown next to your hotbar.
* Press <KeyBind id="key.megatools.swap_tool" /> to swap to that tool, and again to swap back.
* Press <KeyBind id="key.megatools.swap_last" /> to swap to the last tool you used.
* **Shift + scroll** picks between several matching tools.

## Custom rules

Click **Custom Rule** in the [tool menu](tool_menu.md) to choose which blocks pre-select a stored tool.

![The Custom Rules menu of the Mega Tool](swap_rules_gui.png)

* Select a stored tool, then put blocks in its filter slots.
* Left-click a filter to match this block, a block tag or the whole mod. Right-click to remove it.
* **Whitelist / Blacklist** (above the filter slots) switches lists. Blacklist filters (red) stop the tool being pre-selected for those blocks. A more exact whitelist filter still wins: blacklist all of a mod, whitelist one block of it.
* **Copy** and **Paste** move all rules to another tool.
