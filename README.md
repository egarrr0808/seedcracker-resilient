# SeedCracker Resilient

SeedCracker Resilient is an experimental Fabric client mod for Minecraft 26.2.
It is a replacement fork of
[SeedCrackerX](https://github.com/19MisterX98/SeedcrackerX) that can recover a
world seed without trusting the seed-like value sent by a server during login
or respawn.

Use it only on worlds you own or servers where you have permission to test.

## Overview

The project began as a small change to SeedCrackerX: record but do not trust the
seed-like value supplied by a server. It now provides a broader set of tools
for studying vanilla-compatible generation when a server changes that value or
uses a datapack with different structure-placement settings.

Current features include:

- seed recovery from independently observed structures and dungeon/decorator
  evidence;
- custom structure spacing, separation, frequency, and salt settings;
- separate seeds and placement profiles for each server address;
- automatic retries using verified public anti-datapack profiles;
- structure-position recording and spacing/separation analysis;
- proximity-based village observations when the exact start chunk is unknown;
- candidate validation and diagnostic evidence export;
- client-side structure locating without access to the server's `/locate`
  command;
- locating all vanilla 26.2 biomes through SeedMapper; and
- locating potential entity habitats, such as villages and swamp huts for cats.

The mod performs calculations from data already visible to the client. It does
not grant operator permissions, request unloaded chunks, or provide access to
server files.

## Development history

The first version updated the upstream mod for Minecraft 26.2 and introduced
resilient mode. Server-supplied seed hashes, seed-database integration, and
generation evidence considered unsafe for this recovery model were excluded.
Candidate validation and atomic evidence exports were added so failed searches
could be inspected without mixing incompatible observations.

Later work added explicit anti-datapack profiles. Placement settings can now be
stored per server, observations can be rebuilt after a profile change, and
known public profiles can be attempted in sequence. A placement analyzer was
added for unknown static datapacks, together with village proximity evidence
for cases where the true jigsaw start chunk is not visible to a normal player.

The most recent changes added client-side structure locating, vanilla 26.2
biome locating through SeedMapper, spawn-habitat locating, broader structure
support, and automatic fallback from vanilla rules to verified public profiles.
These additions remain local calculations; they do not invoke privileged server
commands.

## Download

[Download the latest Minecraft 26.2 release](https://github.com/egarrr0808/seedcracker-resilient/releases/latest).

Version numbers describe different things:

- **Minecraft 26.2** is the supported game version.
- **SeedCracker Resilient 0.3.0** is this fork's release version.
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
3. Install SeedMapper 2.29.1 to enable exact 26.2 biome and spawn-habitat locating.
4. Put the release jar in the instance's `mods` directory.
5. Remove SeedCrackerX from that instance; this mod replaces it and declares an
   incompatibility with the upstream mod ID.

The jar requires Java 25 and Minecraft `>=26.2 <26.3`.

## Commands

```text
/seedcracker resilient status
/seedcracker resilient on
/seedcracker resilient off
/seedcracker resilient validate <world-seed>
/seedcracker resilient export
/seedcracker resilient antidatapack status
/seedcracker resilient antidatapack auto
/seedcracker resilient antidatapack auto-off
/seedcracker resilient antidatapack profiles
/seedcracker resilient antidatapack default
/seedcracker resilient antidatapack off
/seedcracker resilient antidatapack custom
/seedcracker resilient antidatapack set <structure> <spacing> <separation>
/seedcracker resilient antidatapack set-frequency <structure> <frequency>
/seedcracker resilient antidatapack set-salt <structure> <salt>
/seedcracker resilient antidatapack observe <structure> <chunk-x> <chunk-z>
/seedcracker resilient antidatapack observe-block <structure> <block-x> <block-z>
/seedcracker resilient antidatapack village-here [radius]
/seedcracker resilient antidatapack analyze <structure> [min-spacing] [max-spacing]
/seedcracker data bits
/seedcracker data clear
/seedcracker locate seed <world-seed>
/seedcracker locate <structure> [radius-blocks]
/seedcracker locate biome <biome> [radius-blocks]
/seedcracker locate spawn <entity> [radius-blocks]
```

Seed and anti-datapack settings persist per server address. Joining another
server activates its own vanilla/default/custom profile. Example Trial Chambers
override:

```text
/seedcracker resilient antidatapack set trial_chambers 34 12
/seedcracker resilient antidatapack set-salt trial_chambers 94251327
```

`locate` is entirely client-side and needs no server permission. A successfully
cracked world seed is saved automatically; `locate seed` can set it manually.
It uses the active vanilla/default/custom placement profile, including custom
salts, then filters supported Overworld candidates by biome. Supported IDs are
shown by tab completion. Default radius is 10,000 blocks (maximum 100,000).

With SeedMapper 2.29.1 installed, `locate biome` supports every vanilla 26.2
biome in the current dimension, including cave, Nether, and End biomes. The
reported coordinate is a sampled point inside the biome, not its center.
`locate spawn` checks the world's biome spawn tables and special deterministic
habitats. For example, `locate spawn cat` searches villages and swamp huts.
It locates habitat, not a currently loaded entity or a guaranteed spawn block;
block, light, population-cap, village-bed, and other spawning rules still apply.
Custom datapack biomes and altered biome generation cannot be predicted by the
vanilla 26.2 engine.

## Automatic anti-datapack fallback

`antidatapack auto` starts with vanilla placement. After a complete structure
seed search returns zero candidates, it rebuilds the recorded structure starts
under each verified public profile and retries. Current order is vanilla, then
the published UltimateAntiSeedCracker 1.0.0 ZIP. If all verified profiles fail,
the mod stops cycling and instructs the user to collect exact start chunks with
`observe`, analyze them, and enter a custom profile. It does not cycle merely
because evidence is insufficient, and it never guesses random per-server salts.

Start with anti-datapack placement `off`. If vanilla placement produces no
candidate, `default` activates values from the public
[UltimateAntiSeedCracker 1.0.0](https://modrinth.com/datapack/ultimateantiseedcracker)
ZIP. Changing models resets candidates, then rebuilds recorded structure
observations under the new placement model; old and new modeled evidence never
share one cracking pass. Dungeon/decorator evidence must be recollected. The
profile preserves structure salts in that pack.

Public ZIP values used by current finders:

- buried treasure: frequency `0.012`;
- desert pyramid, igloo, jungle pyramid, swamp hut: spacing `31`, separation `9`;
- End city: spacing `21`, separation `10`;
- ocean monument: spacing `31`, separation `6`;
- shipwreck: spacing `23`, separation `5`; and
- pillager outpost: spacing `32`, separation `8`, frequency `0.22`; and
- village: spacing `33`, separation `9`.

Last item intentionally follows pack file, not Modrinth description: published
1.0.0 ZIP retains outpost `32/8`; description claims `33/7`.

Supported profile overrides are buried treasure, desert pyramid, End city,
igloo, jungle pyramid, ocean monument, pillager outpost, shipwreck, swamp hut,
village, bastion, fortress, Nether fossil, ocean ruin, ruined portal, mansion,
mineshaft, and trial chambers. Exact manual village start chunks are also
accepted as cracking evidence.
Resilient mode still excludes End-city evidence. Pillager-outpost evidence
is recorded for placement analysis but excluded from cracking when frequency
is `0.22`; the current seedfinding library only models vanilla's legacy `0.2`
check. Structures without an existing finder remain unsupported.

## Observing custom placement

Supported finders automatically log start chunks for placement analysis. Exact
manual observations for supported structure IDs also become cracking evidence.
For villages, use `observe village ...` with known *start chunk* coordinates or
`observe-block` with known start-block coordinates. Input must identify actual
structure start, not arbitrary bell/house/waypoint.

For easier village collection, stand inside a village and run `village-here`.
This records a safe proximity constraint within 8 chunks; optional radius is
`1..16`. A visible town-center piece cannot by itself prove the placement chunk
because jigsaw rotation and translation are seed-dependent. Once the seed is
known, `/seedcracker locate village` computes the actual candidate directly.
`analyze` tests candidate grid sizes and reports the full separation range
consistent with sightings. Positive sightings alone cannot identify exact
separation: finite observations may never include the largest possible grid
offset. It also cannot infer frequency or salt. Apply a chosen result with
`set`; recorded structure starts are rebuilt automatically under `custom` mode.

### Manual village workflow

Use one of the following methods for each **different** village. Do not run both
methods for the same sighting.

If the exact village structure-start chunk is unknown, stand within the village
and record a proximity observation:

```text
/seedcracker resilient antidatapack village-here
```

The default radius is 8 chunks. A smaller radius should be used only when there
is reliable evidence that the real start is closer:

```text
/seedcracker resilient antidatapack village-here <radius-from-1-to-16>
```

Run this once per village, then travel to another village and repeat. The
player's current chunk may be any chunk occupied by the village; it does not
need to contain a bell or particular house. The command records a proximity
constraint, not an exact start. More independent villages produce stronger
evidence, but there is no fixed number that guarantees recovery.

If the exact structure-start chunk is known from trustworthy server/debug data,
record it directly:

```text
/seedcracker resilient antidatapack observe village <start-chunk-x> <start-chunk-z>
```

If the exact structure-start block is known instead, use:

```text
/seedcracker resilient antidatapack observe-block village <start-block-x> <start-block-z>
```

Do not substitute a random village chunk, player position, bell, house, or
visual center for an exact start. Use `village-here` when the start is unknown.

Recommended complete sequence:

```text
/seedcracker resilient on
/seedcracker resilient antidatapack auto
/seedcracker resilient antidatapack village-here
/seedcracker resilient status
```

Repeat `village-here` at different villages and allow automatically detected
structures/dungeons to add evidence. If all verified profiles are exhausted,
`analyze` can inspect exact start observations collected automatically or with
`observe`; proximity-only `village-here` observations cannot determine grid
offsets:

```text
/seedcracker resilient antidatapack analyze village
/seedcracker resilient antidatapack custom
/seedcracker resilient antidatapack set village <spacing> <separation>
/seedcracker resilient antidatapack set-frequency village <frequency>
/seedcracker resilient antidatapack set-salt village <salt>
/seedcracker resilient antidatapack status
```

Apply a custom placement only when the output and available evidence support
it. Only set frequency or salt when their values are known; village placement
does not normally need a frequency override. `analyze` can report compatible
spacing/separation ranges, but positive sightings alone cannot discover a salt
or prove one exact separation value. To discard collected cracking evidence
and start again, use `/seedcracker data clear`.

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

Anti-datapack placement support has unit/build coverage but still needs an
in-game controlled-world test. UltimateAntiSeedCracker targets Minecraft
1.20.1–1.20.4, while this mod targets 26.2; its profile is therefore an explicit
compatibility model, not proof that an arbitrary updated/custom pack matches.

Candidate validation establishes consistency with the observations used by the
search. It is not proof of uniqueness unless one candidate remains, and false
or incomplete observations can still prevent a result. Evidence export is a
diagnostic format, not a stable public API.

### When seed recovery can fail

- The server uses arbitrary custom terrain or a generator unrelated to vanilla.
- Structures use unknown independent or randomized per-server salts.
- A plugin moves, removes, or places structures after normal generation.
- The selected Minecraft version or placement profile does not match the
  server.
- Too little independent evidence has been collected, or several seeds remain
  consistent with it.
- An exact observation contains the wrong start chunk. Village bells, houses,
  player positions, and visual centers are not structure starts.
- Evidence was mixed across worlds, dimensions, server resets, or incompatible
  profiles.
- Existing chunks were generated before a datapack/profile change and therefore
  follow different rules from newer chunks.

### When client-side locating can fail

`/seedcracker locate` requires the correct world seed, Minecraft version,
dimension, and generation profile. Its result can be empty or point to ordinary
terrain when any of those inputs is wrong. It can also fail when:

- a datapack changes unsupported placement, biome, exclusion-zone, or terrain
  rules;
- a server plugin disables, removes, relocates, or regenerates a structure;
- the requested structure has no implemented local finder;
- the search radius is too small; or
- old and new chunks were generated with different settings.

Biome locating models vanilla 26.2 generation only. Custom biomes and custom
noise settings are unsupported. Spawn locating reports potential habitat, not
an existing mob or guaranteed spawn: blocks, light, height, beds, mob caps,
difficulty, nearby players, and server plugins still decide actual spawning.

## License and credit

MIT licensed. Based on SeedCrackerX by KaptainWutax and 19MisterX98. The
original copyright and license are preserved in [LICENSE](LICENSE).

This project is an independent fork and is not affiliated with Mojang Studios,
Microsoft, Fabric, or the upstream SeedCrackerX maintainers.
