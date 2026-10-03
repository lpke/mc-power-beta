# Integration decisions

- Options are a new shared screen with independent sidebar/content scrolling,
  collapsible sections, search, draft edits, exact numeric entry and key capture.
  Its layout follows BTA 7.3_04. Native pause-menu button handlers remain intact,
  including incremental world saving and normal quit behavior.
- Module IDs remain unchanged. This preserves mixin integrations, config loading,
  block/item registrations and creative-mode compatibility checks.
- QuickAdditions' sound-manager and title-music mixins are removed from the pinned
  artifact at packaging time. Their music timing, ambient volume and menu-music
  settings remain available. Power Beta owns these hooks, playlist selection,
  channel volumes and safe folder scanning. Other QuickAdditions behavior stays
  in its original artifact. The patch script verifies the source JAR hash first.
- Existing cached sounds load before the legacy resource-download request, with
  bounded network timeouts and duplicate-safe sound pool insertion. This avoids
  a stalled legacy resource server leaving the game silent.
- Settings with unsafe or misleading upstream ranges have reviewed bounds.
  Random-delay ranges cannot be zero; minimum leaf-decay time must be strictly
  below maximum. Recipe and registry settings are marked for restart.
- Creative and building implementations, including durable inventory recovery
  journals and guarded atomic hotbar swaps, remain in their tested components.
  The integration menu never reads or writes item slots to edit settings.
- Component build tools are aligned inside this repository. Frozen standalone
  repositories and the `separate mods final` instance are not modified.
