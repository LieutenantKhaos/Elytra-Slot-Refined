# 26.3 Fabric beta validation

- `:fabric:build` passed with Java 25, Gradle 9.6.0, Loader 0.20.1 and API 0.165.0+26.3.
- Common and Fabric sources compile against 26.3; access widener validation passed.
- 23 injection selectors, argument signatures and explicit invocation targets match the downloaded 26.3 game classes, including client mixins.
- `:fabric:runServer --args="--initSettings"` passed with injection-count checks enabled. The log selected `builtin` with no Trinkets/Curios installed.
- This check initialized server settings only: no EULA accepted or world/gameplay session started.
- Client visuals, inventory operations, flight, migration, and optional integrations need the manual checks in TESTING.md.
- Windows performance-counter warnings were nonfatal. NeoForge release not validated.
