# Item Stats Filter

RuneLite plugin that controls when the **Item Stats** hover tooltips (equipment bonuses, weight, food/potion effects) appear.

- **Hidden items:** the list of items with hidden stat tooltips. Comma-separated names, case-insensitive, `*` as a wildcard (`Prayer potion*`, `*godsword`). Exact names only match exactly, so `Shark` doesn't hide `Raw shark`.
- **Shift-click option:** Shift + right-click an item in your inventory, equipment or bank for **Hide stats** / **Show stats**, which adds or removes that item from the hidden items list. 
    - Items hidden by a wildcard entry don't have a shift-click Hide/Show option.
- **Hidden-stats indicator:** marks an item whose stats are hidden.
    - **Marker box** (default) shows a small `...` box where the stats would be. 
    - **Asterisk** adds `*` after the item name in the Mouse Tooltips box (if that plugin is enabled). 
    - **None** shows no indication that the item has hidden stats.
- **Only show item stats on hold (optional):** hides every item's tooltip until you hold the key. Defaults to Ctrl, can be changed in options.
- **Show hidden item stats on hold:** while the key is held, hidden items show their stats too. Turn it off to keep hidden items hidden even while holding the key.

Other plugins' tooltips (such as item prices) are left alone.

## Setup

Keep the **Item Stats** plugin enabled with its tooltips on and turn on **Item Stats Filter**. All of Item Stats' own settings should keep working as before.