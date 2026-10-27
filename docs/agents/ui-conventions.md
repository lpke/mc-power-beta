# Shared UI conventions

Read this file for UI changes, together with [settings and drafts](settings-ui.md).
Keep these rules here rather than adding style details to the top-level instructions.

## Controls and drafts

- Reuse the main Options controls in other screens and menus. Text and numeric
  fields commit with the check button or Enter and cancel with Escape. Sliders
  support bounded movement, right-click exact entry and individual resets.
  Editors keep isolated drafts.
- Play the native UI click sound for buttons, tabs, toggles and confirmations.
  Slider movement, pointer selection, dragging and group chevrons stay quiet.
- Save and Done controls that commit drafts enable only for changed drafts.
  Rendering, clicks and keyboard acceptance use the same condition. Nested F3
  saves use the reorder draft; naming a new preset counts as a change.
  Navigation-only Done controls stay available, as do Cancel and Discard exits
  for unchanged drafts. The playlist footer hides Cancel for a clean draft;
  Back remains available for navigation.
- Share rendering, pointer, tooltip and clipping geometry. Check compact layouts
  at 320 by 240 as well as wider screens. Keep actionable text and controls inside
  the screen bounds.
- In-group dividers use catalog `separatorAfter` on the preceding setting.
  They share row geometry and never act as headings or input targets.

## Warning overlays

- Confirmation, destructive-action and dirty-draft overlays use centered white
  titles and centered grey detail text. Errors use `0xff8888`. Use `WarningText`
  for consistent wrapping, 11-pixel line spacing and the four-pixel title/detail
  gap. Place its text block 12 pixels above the first action row.
- Keep a `0xc0000000` full-screen dim behind the warning. Retain native buttons,
  their established action order, labels and hit bounds. Use `DraftExitPrompt`
  for Save, Discard and Keep editing when leaving an isolated editor draft.
- Wrap warnings before shortening them. Keep at least 12 pixels of horizontal
  screen padding. Long names and paths must stay inside the overlay; preserve
  the action's consequence before optional identifying details.
- Modal input and hover must not reach the editor behind the warning. Escape
  cancels the warning. Do not add Enter acceptance to destructive actions or
  change draft commit, rollback or callback validation behavior as a style fix.
- Input forms and full-screen recovery or defaults-review pages retain their
  form or document layout. Two-step destructive context actions remain context
  menu rows and follow the menu style below.

## Dropdowns, context menus and checklists

- Choice dropdowns and context action menus share `PopupStyle`: background
  `0xf0202020`, hover/keyboard focus `0xff444444`, selected background `0xff3c3c3c`,
  active blue indicator `0xff268bff`, nine-pixel text padding and vertically
  centered eight-pixel text. Choice dropdowns have a one-pixel `0xff777777`
  border; action menus have no border. Row fills meet the inside of the border.
- Match dropdown row height to its owning control, normally 18 or 20 pixels.
  Share scroll, separator and hit-test geometry through `PopupLayout`.
  One-pixel separators use `0xff555555`, span the full popup width and have no
  vertical margins. Adjacent row fills meet the line. Separators never accept
  input or change callback indices.
- Hover and keyboard focus override the selected fill while retaining its blue
  indicator. Mark the active choice with the blue indicator and the selected
  Defaults-base target with a trailing `Default` label. Context actions open
  without keyboard focus; arrows, Home and End establish focus.
- Track popup ownership by its actual setting, toolbar control or row so only
  that owner retains its open highlight. Dropdowns and context menus own pointer
  hover while open, including nested editor popups and map/photo labels.
  Suppress the underlay pointer and render the popup with real coordinates.
  Truncated playlist choices show their full names.
- Map, minimap and photo checklists retain their toggle layout and stay open
  after a choice. Their colors and text spacing follow the same menu style.

## Sliders and audio rows

- Shared sliders use an eight-pixel thumb contained within the track at both
  endpoints. Exact-entry inputs and their check button occupy the same outer
  bounds, including the input border. The check ends at the slider's right edge.
- Volume sliders use `VolumeControls`: a 150-pixel column where space permits,
  shared by rendering, drag hit bounds, tooltips and exact entry. Place preview,
  related-control and editor buttons before the column rather than shortening it.
  Compact form rows place adjacent actions beside the label.
- Song rows use up to 134 pixels, matching the child sound category width.
  Library groups use up to 150 pixels with the same right edge. Both adapt down
  to 54 pixels on compact screens to retain readable names, independently of
  nearby buttons. Parent and child music sliders share their right edge.
  Library groups place their count before the slider and the folder delete
  button at the far right. Grouped playlists retain their X/Y count at the right.
- Normal Audio category rows use a 16-pixel left inset beneath Game sounds.
  Search results omit normal row indentation. Music and Game sounds place Edit
  and All before the control column, or beside the label at compact widths.
  Extra sounds starts collapsed in Audio.

## Music lists and dragging

- Playlist catalog rows use `MusicRowLayout` for their hover, clipping and
  pointer bounds. The hover area runs from content left minus two pixels to
  content right plus four pixels. Keep the last action four pixels from its
  right edge.
- Active playlist names use gold text and the Defaults base marker: two pixels
  wide, ten pixels high, starting at content left plus one and one pixel above
  the text. Keep text at content left plus eight. Other names
  use white text. Keep trailing Use, Duplicate and Delete controls. Disable Use
  for the active playlist and Delete for protected playlists. Choice popups
  retain their blue active indicator.
- Read-only playlist headers use white text at the input's text inset and a
  bottom `0xff555555` line at the editable input's outer bounds.
- Music lists share `MusicListDrag`: retain the pressed source until movement
  reaches the eight-pixel radial threshold. Suppress row hover after dragging
  starts. Use its shared blue insertion line and pointer-following label, drawn
  after removing the list clip. Keep labels inside the screen bounds.
- Reordering captures immutable source IDs and validates the owning list before
  committing on release. Motion alone never changes the saved order. Cancel on
  focus loss, navigation or an invalid source. Preserve scrolling while dragging.
- Native Beta mouse motion and wheel events call `mouseReleased` with button -1.
  End scrollbar ownership only on its originating button release, focus loss or
  navigation. Apply final release coordinates before clearing the drag.

## Text input and search

- Text inputs share editing and pointer selection through the input bridge.
  UI cursor blinking is separate from chat. Cancel selection on focus or screen
  changes; keep native limits, validation and gcapi3 widget methods.
- Search-only cluster suffixes never change IDs. Search group chevrons toggle
  silently; collapsed titles expand and expanded titles navigate.
