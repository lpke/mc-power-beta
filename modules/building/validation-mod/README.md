# Building fixture

Development only. This fixture automatically creates laboratory worlds and changes
inventory, blocks, settings and bindings. Never install it in a real instance or
ship its JAR.

Build the pack first, then run `./gradlew -p modules/building/validation-mod
--no-daemon build` from the repository root. Install alongside Power Beta only in
a disposable clone.

Results go to `building-validation.log`. Additional commands go in
`building-validation.command`; inspect the dispatch in the fixture source for
setup requirements. It covers slab placement, hotbar transactions, camera input,
creative flight and editor integration. Use the root `validation-mod` for current
Options and audio checks.
