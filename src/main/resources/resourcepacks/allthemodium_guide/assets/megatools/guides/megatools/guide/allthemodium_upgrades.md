---
navigation:
  title: Allthemodium upgrades
  icon: megatools:mega_tool_unobtainium
  parent: index.md
  position: 25
item_ids:
- megatools:mega_tool_allthemodium
- megatools:mega_tool_vibranium
- megatools:mega_tool_unobtainium
---

# Allthemodium upgrades

<Row>
  <ItemImage id="megatools:mega_tool_allthemodium" scale="2" />
  <ItemImage id="megatools:mega_tool_vibranium" scale="2" />
  <ItemImage id="megatools:mega_tool_unobtainium" scale="2" />
</Row>

This modpack has the **Allthemodium** mod, so the [Mega Tool](mega_tool.md) can be upgraded three times:
Mega Tool → **Allthemodium** → **Vibranium** → **Unobtainium**.

* **Everything stays**: stored tools, Enchantment Plates, torches, the Support Tool, settings and custom
  rules move over to the upgraded tool.
* Every tier works exactly like the Mega Tool: instant mining, Delayed Break, every paxel ability,
  36 slots of [tool storage](tool_storage.md), attack speed 3.5, never wears out and doesn't burn.
* Each upgrade adds attack damage and lets the tool break tougher Allthemodium ores.

## Attack damage and what each tier breaks

| Tool | Attack damage | Breaks |
|---|---|---|
| Mega Tool | 28 | everything except vibranium and unobtainium ore |
| Mega Tool – Allthemodium | 38 | everything except unobtainium ore |
| Mega Tool – Vibranium | 48 | every block, including unobtainium ore |
| Mega Tool – Unobtainium | 60 | every block |

A tool that is too weak for a block says which upgrade it needs above the hotbar, for example
*Needs a Vibranium Mega Tool or better*. The attack damage values are the defaults; a modpack can change
them in `megatools-startup.toml`, and modpack developers can change which blocks need which tier (see
[For modpack developers](modpack_developers.md)).

## Recipes

<RecipeFor id="megatools:mega_tool_allthemodium" fallbackText="The Mega Tool – Allthemodium has no recipe in this modpack." />

<RecipeFor id="megatools:mega_tool_vibranium" fallbackText="The Mega Tool – Vibranium has no recipe in this modpack." />

<RecipeFor id="megatools:mega_tool_unobtainium" fallbackText="The Mega Tool – Unobtainium has no recipe in this modpack." />
