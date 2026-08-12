# SeedCracker Resilient 0.2.0 — Minecraft 26.2

First public release of the resilient SeedCrackerX fork.

## Highlights

- Treats the login/respawn seed-derived value as untrusted and excludes it from
  candidate generation.
- Disables SeedCrackerX database lookup and submission in resilient mode.
- Applies a conservative evidence policy and keeps dungeon evidence available
  for full 64-bit seed lifting.
- Adds `/seedcracker resilient` status, toggle, validation, and export commands.
- Writes debounced, atomic JSON evidence snapshots for diagnosis and auditing.
- Validates candidates against the committed cracking snapshot, preventing a
  queued-observation race.
- Handles partially initialized dungeon observations during evidence export.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer compatible loader
- Fabric API for 26.2
- Java 25

Remove upstream SeedCrackerX before installing this jar. The two mods are
declared incompatible.

Use only on worlds you own or servers where you have permission to test. Read
the repository's detailed article for the algorithm, trust model, and limits.
