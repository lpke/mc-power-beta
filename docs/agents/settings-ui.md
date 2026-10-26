# Settings, defaults and shared UI

- `PowerConfig` is the only settings writer: `config/power-beta.json`.
  Rule groups identify world scope without repeated row labels.
  Defaults and interrupted-save recovery run from the language adapter **before**
  mixin plugins and configuration entrypoints. Preserve stable setting, world,
  track and preset IDs. Global schema 9 and world-rule/template format 4 are the
  supported formats. Older configuration schemas require an explicit backed-up conversion to schema 9 and music-data format 3. Older rule formats and pre-rule worlds require separate native validation. Reject unsupported data with recovery guidance;
  never reconstruct permissions from retired global settings. Retain opaque historical
  evidence already stored in configuration documents without consuming it.
  Validate converted values and verify durable backups before replacing originals.
  Unsupported/future formats block writes or world opening with a useful recovery UI.
  Fresh installs copy `defaults/power-beta.json`; keep unrelated historical importers out.
  Document current/supported formats in release notes whenever either format changes.
  Gameplay hotkeys queue settings snapshots through `PowerConfig`; never flush files
  on the input or render path. Flush queued saves before menu backups and on shutdown.
  The config API retains its `gcapi3` binary identity for external dependencies.
- Saved-world rule edits use the shared registry and editor without loading a world. Back up and verify metadata, reject concurrent changes and unsupported data, and preserve player data, game mode, the previous metadata file.
- The game uses Gson 2.8.9. Do not rely on record deserialization without an explicit
  adapter. Test persisted data with that version, including rollback and restart.
- Options edits use `ConfigSession`. Auto-apply defaults on; manual Apply commits and
  Cancel restores previews. Dirty exits offer Save, Discard and Keep editing. Creation
  edits affect only the future-world draft. Main-menu templates, creation and loaded
  world rules share search and editor behavior. Preserve Custom drafts when cycling
  presets. Custom starts in Survival; block the Cheats preset visibly if its
  Creative starting mode lacks permission.
  Update linked values together. Preserve search, scroll, related-view history and
  modifier ownership when rebuilding rows. Share render and hit-test geometry.
- Settings in other screens and menus reuse the main Options controls and behavior:
  inline text/numeric editing with check/Enter commit and Escape cancel, bounded
  sliders with right-click exact entry, individual resets and isolated editor drafts.
  Play the native UI click sound for buttons, tabs, toggles and confirmations.
  Slider movement, pointer selection, dragging and silent group chevrons stay quiet.
  Save/Done controls that commit isolated drafts enable only for changed drafts;
  rendering, clicks and keyboard acceptance share that condition. Nested F3 saves
  use the reorder draft, and naming a new preset counts as a change. Navigation-only
  Done controls stay available, as do Cancel/Discard exits for unchanged drafts.
- Choice dropdowns and context action menus share `PopupStyle`: background
  `0xf0202020`, hover/keyboard focus `0xff444444`, selected background `0xff3c3c3c`,
  active blue indicator `0xff268bff`, nine-pixel text padding and vertically centered
  eight-pixel text. Choice dropdowns have a one-pixel `0xff777777` border; action
  menus have no border. Row fills meet the inside of the dropdown border without
  an inset. Match dropdown row height to its owning control, normally 18 or 20
  pixels, and share all scroll, separator and hit-test geometry through `PopupLayout`.
  One-pixel separators use `0xff555555`, span the full popup width and have no vertical
  margins. Adjacent row fills meet the line; separators never accept input or change callback indices.
  Hover and keyboard focus override the selected fill while retaining its blue indicator.
  Mark the active choice with the blue indicator and the selected Defaults-base
  target with a trailing `Default` label. Context actions open without keyboard
  focus; arrows, Home and End establish focus. Track popup ownership by the actual
  setting, toolbar control or row so only that owner retains its open highlight.
  Map, minimap and photo checklist panels keep their existing toggle layout and
  persistent-open behavior; their colors and text spacing follow the same menu style.
  Master and category volume sliders retain their full control width. Only Music
  and Game sounds reserve space for their Edit and All buttons. Search results omit
  normal row indentation. Extra sounds starts collapsed in Audio.
  Parent and child music group sliders share horizontal bounds. Place group counts
  after the slider, anchored to the right. Playlist catalog rows use the Defaults picker's
  gold bar and text for the active playlist, and retain disabled Delete buttons for
  protected playlists. Choice popups retain their shared blue active indicator.
  Shared sliders use an eight-pixel thumb contained within the track at both
  endpoints. Exact-entry inputs and their check button occupy the same outer
  bounds, including the input border, and the check ends at the slider's right edge.
- Reset values depend on the user's Defaults base: Power Beta, Refined Beta 1.7.3
  or Beta 1.7.3. Power Beta keeps the `power_beta` ID; retired base IDs are rejected
  without reloading or resetting settings. Keep the backend/catalog
  fallback values stable so changes to one base do not change another. Power Beta's
  explicit overrides and fresh-install JSON must agree. Never copy personal controls,
  paths, music libraries, HUD layouts or world IDs into defaults. Ore vein mining
  starts with ores only; Creative/Spectator use modern response and standard speed.
  All three flight modes default sprint activation to Toggle in every base.
  All bases start at GUI scale 4. `DefaultBases` resolves stable setting IDs against the explicit
  `default-bases.json` overrides; retain each setting's shared fallback default
  when copying drafts. Use as base changes reset targets and unedited creation
  templates together; preserve global preferences, customized templates and unsupported data.
  Load resets global preferences and creation templates, never existing world rules
  or recovery data. Saved music, title and F3 presets remain user data. Keep reset
  controls in every editor consistent with the selected base. Existing installations
  skip the first-launch chooser; fresh exports keep setupComplete false until chosen.
- `settings-layout.json` owns tab/group order; catalog `order` values arrange
  settings inside groups. Presentation metadata applies to every adapter without
  changing storage IDs. Keep the flight sprint binding shared; Creative, Spectator and freecam own separate sprint preferences.
  The focus-loss pause preference is `power_controls:general.pauseOnLostFocus`.
- Numeric display units do not change stored values. Percentages belong in slider
  values, not setting names; exact-entry fields retain plain numeric text and convert
  through the same display units. Duration editors use seconds;
  processing budgets remain per tick. Validate item/filter IDs before accepting
  drafts, and keep temporary errors outside row layout.
- Paired color and opacity controls keep their original storage IDs and types, share
  one picker draft and reset/Undo boundary, and hide only the paired opacity row.
  `ColorValue` preserves legacy palette indices 0–3; custom palette colors use
  `0x1000000 | RGB`, including black and RGB values 1–3.
- Native Beta mouse motion and wheel events call `mouseReleased` with button -1.
  Scrollbar ownership ends only on its originating button release, focus loss or
  navigation; apply final release coordinates before clearing the drag.
- Text inputs share editing and pointer selection through the input bridge. UI cursor
  blinking is separate from chat. Cancel selection on focus/screen changes; keep native
  limits, validation and gcapi3 widget methods. Search-only cluster suffixes never change IDs.
  Search group chevrons toggle silently; collapsed titles expand and expanded titles navigate.
- Item and block settings use the shared graphical picker. Keep each editor draft local
  until Done, validate custom IDs against loaded registries, retain metadata variants
  where the stored format supports them, and never touch gameplay inventories.

- Block-rule and item-picker exits confirm unsaved drafts, including nested tool selections.
  Modal input must not reach the editor behind it. Underwater overlay overrides affect
  only rendering; full bright temporarily hides that texture without changing its preference.
  Full bright uses normal terrain fog underwater, preserving fog-distance controls and
  native water physics, lava fog and cloud fog. Disabling it restores native water fog.
  The water-material override is scoped to fog rendering on its owning thread, including
  StationAPI's color hooks; it must not affect other fluid queries.

- In-group dividers use catalog `separatorAfter` on the preceding setting; they share row geometry and never act as headings or input targets.
- Creation-template review revisions are independent of configuration and world-rule
  formats. Preserve unsupported template documents for review while unrelated
  preferences remain readable. Use RuleDefaults for selected-base targets and
  acknowledged revisions; strict RuleSet readers still reject incompatible values.
  Advance RuleDefaults.REVISION and RELEASE whenever creation-rule schemas or
  predefined rule defaults change, so customized templates receive the new review.
  Reset and Keep require conditional, backed-up PowerConfig section replacement.
- Options search scope is visit-independent session state, restricted to ordinary Options.
  Search breadcrumbs and tab filtering use the actual available editor destination; map
  sidebars and isolated rules editors retain their own fixed search scopes.
  All ordinary tabs share one All/This tab choice, starting at All. Controls always
  searches controls, has no scope button, and never changes the shared choice.

- Standalone creation/saved-world rule editors do not own music widgets. Shared render,
  tooltip and input paths must check the widget is active before accessing it.
  Continuous mining removes only the singleplayer empty-swing cooldown at its source;
  preserve unrelated attack delays, native damage, Creative held repeats and multiplayer.
  Map spawn-light shading uses its own radius and bounded, tick-owned loaded-terrain
  surveys; rendering reads snapshots and disabled maps perform no survey work.
