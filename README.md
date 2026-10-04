# Item Stats Filter

RuneLite plugin that controls when the **Item Stats** hover tooltips (equipment bonuses, weight, food/potion effects) appear.

- **Hidden items:** a list of items that never show a stat tooltip. Comma-separated names, case-insensitive, `*` as a wildcard (`Prayer potion*`, `*godsword`). Exact names only match exactly, so `Shark` doesn't hide `Raw shark`.
- **Shift-click option:** Shift + right-click an item in your inventory, equipment or bank for **Hide stats** / **Show stats**, which adds or removes it from the list. Items hidden by a wildcard entry don't have this option.
- **Only show while key held (optional):** tooltips only appear while a key is held. Defaults to Ctrl, can be changed in options. Hidden items will still have hidden stats with this enabled.

Other plugins' tooltips (such as item prices) are left alone.

## Setup

Keep the **Item Stats** plugin enabled with its tooltips on and turn on **Item Stats Filter**. All of Item Stats' own settings should keep working as before.