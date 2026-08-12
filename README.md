# SeedCracker Resilient

SeedCracker Resilient is an experimental Fabric client mod for Minecraft 26.2.
It is a replacement fork of
[SeedCrackerX](https://github.com/19MisterX98/SeedcrackerX) that can recover a
world seed without trusting the seed-like value sent by a server during login
or respawn.

Use it only on worlds you own or servers where you have permission to test.

## Download

[Download the latest Minecraft 26.2 release](https://github.com/egarrr0808/seedcracker-resilient/releases/latest).

Version numbers describe different things:

- **Minecraft 26.2** is the supported game version.
- **SeedCracker Resilient 0.2.0** is this fork's release version.
- **SeedCrackerX 2.16.1** identifies the upstream codebase used for the first
  release; it is not the supported Minecraft version.

## What resilient mode changes

Resilient mode is enabled by default. It:

- records, then ignores, the server-supplied hashed seed;
- excludes End cities, End pillars, End gateways, and client-derived biome data;
- keeps Overworld structures and dungeons as independent observations;
- disables seed-database lookup and submission;
- resets collected evidence whenever the mode changes;
- validates candidates against evidence already committed to the search; and
- atomically exports debounced JSON evidence snapshots.

This policy is aimed at servers that alter the login seed field but still use
vanilla-compatible world generation. It cannot recover a meaningful vanilla
seed from arbitrary custom terrain, per-feature seeds, or modified structure
placement.

Read [How resilient seed recovery works](docs/how-resilient-seed-recovery-works.md)
for the short explanation, detailed algorithm, trust model, and limitations.

## Install

1. Install Fabric Loader for Minecraft 26.2.
2. Install Fabric API for Minecraft 26.2.
3. Put the release jar in the instance's `mods` directory.
4. Remove SeedCrackerX from that instance; this mod replaces it and declares an
   incompatibility with the upstream mod ID.

The jar requires Java 25 and Minecraft `>=26.2 <26.3`.

## Commands

```text
/seedcracker resilient status
/seedcracker resilient on
/seedcracker resilient off
/seedcracker resilient validate <world-seed>
/seedcracker resilient export
/seedcracker data bits
/seedcracker data clear
```

`status` reports structure, lifting, and decorator progress, plus the number of
distinct server hashes ignored in the current collection run. Evidence is
automatically exported after new observations to:

```text
config/seedcracker-resilient/evidence/<server>.json
```

The export contains the observed hash values, evidence coordinates, estimated
progress, and current candidates. Treat it as sensitive: coordinates and a
server address can identify a private world.

## Build and test

Minecraft 26.2 requires Java 25:

```bash
export JAVA_HOME="$HOME/.minecraft/runtime/java-runtime-epsilon/linux/java-runtime-epsilon"
./gradlew clean test build
```

The release jar is written to `build/libs/seedcracker-resilient-*.jar`.

## Status and limitations

This release has been tested against a controlled Paper 26.2 server whose
plugin changed the client-visible seed field. The client ignored that field and
recovered the server's real seed from structure and dungeon observations.

Candidate validation establishes consistency with the observations used by the
search. It is not proof of uniqueness unless one candidate remains, and false
or incomplete observations can still prevent a result. Evidence export is a
diagnostic format, not a stable public API.

## License and credit

MIT licensed. Based on SeedCrackerX by KaptainWutax and 19MisterX98. The
original copyright and license are preserved in [LICENSE](LICENSE).

This project is an independent fork and is not affiliated with Mojang Studios,
Microsoft, Fabric, or the upstream SeedCrackerX maintainers.
