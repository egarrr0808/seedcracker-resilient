# How resilient seed recovery works

SeedCracker Resilient does not reverse a fake seed and does not ask the server
for the real seed. It treats the loaded world itself as evidence. Vanilla world
generation is deterministic, so the locations of structures and decorators
constrain the random seed that could have produced them.

Use this material only for worlds you own or have explicit permission to test.

## The principle

A Minecraft server must send clients enough chunk data to render and play the
world. A client can therefore observe facts such as “a shipwreck starts in this
chunk” or “a dungeon exists at these block coordinates.” Those facts are not
the seed, but each one eliminates seeds whose generator would put the feature
somewhere else.

The search is an intersection of constraints:

```text
all possible seeds
  ∩ seeds matching structure observation 1
  ∩ seeds matching structure observation 2
  ∩ ...
  ∩ seeds matching full-seed dungeon observations
  = remaining candidate world seeds
```

The “bits” shown by the mod are an estimate of how much a feature narrows the
search. They are information estimates based on placement probabilities, not
cryptographic security bits and not a guarantee that every observation is
independent.

## Why changing the login seed is insufficient

The Minecraft login and respawn data include a client-visible, seed-derived
`long`. Normal SeedCrackerX can use that value as another constraint when it
lifts a 48-bit structure seed into a 64-bit world seed. A protection plugin can
replace this field, and a trusting client may then search the wrong branch or
reject the real seed.

That packet value is only metadata. Changing it does not move a temple,
shipwreck, monument, or dungeon already generated in the chunks sent to the
player. As long as the server still uses compatible vanilla generation, those
positions remain constraints on the real generator state.

Resilient mode handles the packet as untrusted input. It records distinct
values for diagnostics, clears the value from active cracking state, skips
database lookup, and never uses it to generate candidates. This is why a fake
value and a recovered world seed can be entirely different without being a
contradiction.

## The search in depth

### 1. Collect observations

When a chunk finishes loading, SeedCrackerX finders inspect its blocks and
structure pieces. Recognized features become evidence records containing the
feature type and coordinates, plus feature-specific data when needed.

Resilient mode uses a conservative policy. It disables End-city, End-pillar,
End-gateway, and biome finders while leaving compatible Overworld structure
finders and dungeon collection enabled. The excluded observations are useful in
other cracking paths, but they broaden the trust surface when the server is
deliberately supplying misleading seed data.

### 2. Recover the 48-bit structure seed

Many Java Edition structure-placement decisions use the lower 48 bits of the
world seed through Java's linear congruential random generator. In simplified
form, a structure's candidate position in a region is derived from:

```text
lower48(worldSeed) + regionX·A + regionZ·B + featureSalt
```

The exact constants, offsets, spacing rules, and random calls differ by feature
and Minecraft version. The bundled seedfinding libraries reproduce those
rules. For every candidate, the mod asks whether the simulated feature would
start in the observed chunk.

For “liftable” structures, partial placement information is used to reject
large groups of lower-48-bit candidates early. The implementation first checks
small lower-bit ranges, extends surviving prefixes, and then applies all
compatible structure observations. Other structure data further reduces the
result set. The practical goal is enough independent evidence to leave one or
a small number of 48-bit structure seeds without naively retaining all
`2^48` possibilities.

The status thresholds reflect the upstream search strategy: 40 estimated
lifting bits and 32 regular structure bits. More evidence can be required when
constraints overlap or several candidates collide.

### 3. Lift 48 bits to the full 64-bit world seed

A structure seed does not uniquely identify a Java world seed. It leaves 16
upper bits unknown, so each surviving structure seed has up to `2^16 = 65,536`
full-seed extensions:

```text
worldSeed = (upper16 << 48) | structureSeed
```

For Minecraft 1.18 and newer, the fork tests those extensions with decorator
observations that depend on the full world seed. In the controlled 26.2 test,
dungeon positions supplied enough full-seed information for the search to
select the real candidate without consulting the server-provided hash.

### 4. Validate without racing the search

Generated candidates are checked again against the committed evidence set. A
critical detail is that newly detected observations enter a pending queue while
a cracking pass may still be running. Validating against pending data could
delete a candidate produced from the previous committed snapshot and trigger a
search loop. This fork validates only against evidence committed to that pass;
the next pass incorporates queued observations deterministically.

The explicit command below applies the same consistency check:

```text
/seedcracker resilient validate <world-seed>
```

`PASS` means no tested observation contradicted the candidate and at least one
observation matched. Exceptions are reported as inconclusive rather than being
silently counted as matches. A pass is evidence of consistency, not a proof
that no other seed can match.

### 5. Export an audit trail

Evidence snapshots are serialized on a single background thread after a short
debounce. The writer creates a temporary file and atomically replaces the
destination where the filesystem supports it. This avoids exposing a partially
written JSON document to tools that watch the export directory.

Each snapshot contains:

- schema and Minecraft versions;
- the server identifier used for the filename and record;
- estimated structure, lifting, and decorator progress;
- ignored client-visible hash values;
- current structure- and world-seed candidates; and
- normalized evidence records and feature-specific fields.

Partially initialized observations are exported with unavailable fields set to
`null`; one incomplete dungeon record cannot abort the entire snapshot.

## What the controlled test demonstrated

The test server ran Paper 26.2 with a plugin that replaced the seed-derived
value visible to the client. The client log showed one ignored server hash,
enough structure/lifting evidence, more than the required decorator evidence,
and a single recovered candidate. The candidate matched the seed reported from
the server console.

This demonstrates a narrow result: altering only the client-visible seed field
does not conceal a vanilla-compatible world seed once enough independent world
generation evidence is available. It does not show that every protection
plugin, custom generator, Minecraft version, or server configuration can be
handled.

## Failure modes and boundaries

- Custom terrain or structure generation may have no corresponding vanilla
  seed, so the model can produce no candidate or a meaningless one.
- Per-world, per-dimension, or per-feature seed systems break the assumption of
  one shared 64-bit world seed.
- Incorrect finder detections create contradictory evidence. Clear the data and
  recollect if validation reports mismatches.
- Too little or correlated evidence leaves multiple candidates.
- Version mismatches change salts and generation rules. This release targets
  Minecraft 26.2 only.
- Servers can limit what chunks a player legitimately receives. This mod does
  not grant access to unloaded or unauthorized server data.
- Exported evidence includes coordinates and a server identifier. Review it
  before sharing publicly.

## Design summary

The protection boundary is simple: do not trust a server-controlled hint when
the observable deterministic world provides independent constraints. The fork
does not make the cracking mathematics fundamentally new; it changes which
inputs are trusted, prevents external database interaction, makes candidate
checks deterministic, and leaves an inspectable evidence record.

The implementation is derived from SeedCrackerX and uses its existing finder,
seedfinding, and lattice-reversal machinery. See the repository history and
`LICENSE` for attribution.
