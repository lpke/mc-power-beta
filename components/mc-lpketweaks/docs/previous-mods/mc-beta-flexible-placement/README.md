# Beta Flexible Placement

Singleplayer placement controls for Minecraft Beta 1.7.3 on Babric.

Open **Mods > Beta Flexible Placement > Configure > Hotkeys**, or **Options >
Controls**, to bind the controls. All keys start unbound to avoid conflicts.
The mod starts enabled; placement changes only while a modifier is held unless
Place against containers is enabled separately.

| Control | Action |
| --- | --- |
| Placement offset, hold | Shows a five-region overlay. Aim at an edge to place diagonally; the centre leaves a one-block gap. |
| Placement adjacent, hold | Places next to the selected edge in the clicked block's plane. Useful for bridging from the top face. Centre keeps normal placement. |
| Placement rotation, hold | Faces supported directional blocks toward the selected overlay region. |
| Placement reverse, hold | Reverses the normal facing, including upward/downward pistons. |
| Placement into face, hold | Points supported blocks into the clicked face. Combine with Reverse to point out. |
| Flexible placement, toggle | Enables/disables these controls. |

The preview marks the destination in green or red. Native reach, entity
collision and block support rules still apply. Pistons support all six
orientations. Dispensers, furnaces, stairs, pumpkins and repeaters stay horizontal.
Ordinary block items and repeaters support these controls. Doors, beds, buckets,
vehicles and editor tools use their existing behaviour.

Settings include overlay visibility, destination preview, four overlay colours,
10–90% opacity and placement against containers. While a modifier is held,
placement bypasses the clicked container's use action. The separate container
setting extends that behaviour to ordinary block placement.

Offset and Adjacent displacements add. Into overrides Rotation; Reverse applies
last. Release modifiers after opening a menu, losing focus or changing worlds.
Freecam, death and multiplayer suspend placement changes.

**Done** saves; **Cancel** or Escape discards the draft. Configuration is in
`config/beta-flexible-placement.properties`; keys are shared with native
`options.txt`. Files are saved atomically. No world formats, block IDs or packets
are added. Removing the JAR requires no world conversion.

Beta Fast Place 1.1.0 tracks altered destinations and gives held flexible controls
priority over its remembered yaw. It remains optional. See [research](RESEARCH.md)
for the upstream implementation and Beta differences, and [validation](VALIDATION.md)
for tested behaviour.

## Build

Use JDK 21 with the included wrapper:

```sh
./gradlew --no-daemon build
```

Install `build/libs/beta-flexible-placement-1.0.0.jar`. The sources JAR and the
separate development validation JAR are not needed for gameplay. Java 17 or
newer is required at runtime. Mod Menu is optional.
