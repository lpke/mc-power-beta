# Power Beta development

- Read `docs/upstreams.md` before checking upstream mods for updates. It distinguishes our source-built utility modules from intentionally retained platform/performance binaries.
- Keep settings and messages cohesive. Group settings by function and use functional labels without upstream mod branding.
- Clone the latest development Prism instance before testing. Never edit or launch the user's active instance, and never terminate a process unless its exact PID and working directory identify the disposable instance you started.
- Preserve `separate_mods_final` and the separate-mod repositories as the frozen checkpoint. Make pack changes here.
- Build with Java 21 through `python3 tools/build_pack.py`. Run Loom builds sequentially because they share mapping caches.
- Test modifiers with physical key-down input, both release orders, focus changes and menus. Only Ctrl, Shift and Alt are supported. Gameplay input must never wait for key-up or a chord timeout.
- Treat inventory movement as a transaction. Keep item identity, counts, damage and NBT intact; retain recovery journals and failure shutdown. Settings edits must never move inventory items.
- Commit/push only when the user authorizes it. The initial Power Beta delivery request authorizes committing and pushing this work to the private repository.
