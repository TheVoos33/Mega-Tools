package com.thevoos.megatools.recipe;

/**
 * Marker for recipes that keep a consumed Mega Tools tool's contents in the result
 * (carry-over recipes, repair recipe). ToolCarryOver#onItemCrafted skips its "give the stored items
 * back" safety net for these; refunding as well would duplicate the items.
 */
public interface PreservesToolData {}
