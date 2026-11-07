# Polytone in WesterosBlocks

Reference for agents and contributors who touch colormaps, particles or block
models. Polytone is a client-side mod in the modpack. It is not a Gradle
dependency (only `suggests` in `fabric.mod.json`), so nothing here runs in the
`runClient` dev client.

## Where to look things up

- Wiki: <https://github.com/MehVahdJukaar/polytone/wiki>. The wiki tracks the
  newest Polytone (26.x). Many pages mark features as `1.21.11+` or `1.21.1 and
  below`. We are on Minecraft 1.21.1, so skip anything marked newer.
- Source for our version: branch `1.21.1` of
  <https://github.com/MehVahdJukaar/polytone> (`mod_version = 1.21-5.3.5` when
  this was written). When the wiki and the 1.21.1 code disagree, the code
  wins. The places they disagree that matter to us are listed below.
- Most useful wiki pages: Shared-Concepts (targeting, conditions), Colormaps,
  Block-Properties-Modifiers, Fluid-Properties-Modifiers, Custom-Particle-Types,
  Scripting-Expressions, Models-Improvements.
- Reload in game with F3+T. Polytone logs to `logs/polytone.log` as well as
  `latest.log`.

## What we ship

| Path | What it is | Who writes it |
|---|---|---|
| `assets/westerosblocks/polytone/colormaps/*.png` | 26 standalone colormaps, 256x256, no `.json` | hand-made |
| `polytone/block_modifiers/<colormap>_tinted_blocks.json` | `targets` list + `"colormap": "westerosblocks:<colormap>"` | `ModPolytoneProvider` (datagen) |
| `polytone/item_modifiers/<colormap>_tinted_blocks.json` | same content, tints the block items | `ModPolytoneProvider` (datagen) |
| `assets/westerosblocks/polytone/block_modifiers/*_particle_emitter.json` | `particle_emitters` for the 5 emitter blocks | hand-made |
| `assets/westerosblocks/polytone/custom_particles/*.json` | 6 custom particle types | hand-made |
| `assets/westerosblocks/particles/*.json` | sprite lists for those particles (vanilla format) | hand-made |
| `assets/minecraft/polytone/fluid_modifiers/water.json` | water tint and fog use `westerosblocks:water` | hand-made |

The generated files go to `src/main/generated/`, which is gitignored. Run
`runDatagen` before building or they are missing from the jar.

### Colormaps

- A block opts in with `colorMult` (or `colorMults`) in
  `definitions/WesterosBlocks.json`, for example
  `"colorMult": "westerosblocks:stone"`, which names
  `polytone/colormaps/stone.png`. The provider drops the namespace and keeps
  the last path segment, so the older spelling `textures/colormap/stone` still
  works, but write new entries as ids.
- Vanilla blocks are recoloured through `definitions/color_maps.json`
  (`ColorMapLoader`). Entries whose `colorMult` is a hex colour (`#...`) are
  skipped by the provider.
- A block with `colorMults` gets one indexed colormap instead: entry N of the
  list tints `tintindex` N (`bushy_grass_*`: 0 = grass, 1 = foliage). These
  go in `<id0>__<id1>_tinted_blocks.json`.
- Blocks in the test creative tab are skipped.
- A `.png` in `colormaps/` with no `.json` becomes a colormap that samples
  biome temperature (x) and downfall (y), with the triangular lookup vanilla
  grass uses (`Colormap.createDefTriangle()` in 1.21.1). To change sampling,
  add `colormaps/<name>.json` with `x_axis`, `y_axis` and so on (see the
  Colormaps wiki page).
- A colormap only tints faces that have a `tintindex`. Models built by
  datagen get `tintindex: 0` when the block is tinted (`CuboidModelBuilder`,
  `LayerBlockExporter`). A plain (non-indexed) colormap applies to every tint
  index.
- To add a colormap: add the PNG to `polytone/colormaps/`, point a block's
  `colorMult` at it, run `runDatagen`.

### Particle emitters

`WCParticleEmitterBlock` (block type `particle`) spawns nothing itself. Polytone
finds `polytone/block_modifiers/<blockName>.json` by implicit targeting (file
path = block id) and runs its `particle_emitters` each animate tick. Each
emitter there names a `westerosblocks:<particle>` defined in
`custom_particles/` with a sprite list in `particles/`. The ids in the three
places must match.

To add an emitter: a definition with `"blockType": "particle"`, a
`block_modifiers/<blockName>.json`, a `custom_particles/<particle>.json`, and a
`particles/<particle>.json` plus textures.

### Model rotations

Polytone removes vanilla's limits on model rotation (Models-Improvements wiki
page):

- Element `rotation.angle` can be any value, not only -45/-22.5/0/22.5/45. We
  use 135, -135, 180, -180, 26.5, -20, ±12.5 and ±37.5 (`beam_brace*`,
  `*_step_stairs`, `template_hedge_wall_*`, `ladder/steep*`,
  `awnings/awning_slanted*`, `wall_kite_shield_stark`).
- Element rotation can use `{"x":..,"y":..,"z":..,"origin":[..]}` instead of
  `axis` + `angle`. The `cabbage` and `purple_cabbage` models use this.
- Blockstate variants may also use `z`, any angle for `x`/`y`/`z`, and
  `xoffset`/`yoffset`/`zoffset`. Nothing here uses those yet.

These are intentional. Do not "fix" them. The `runClient` log errors about these
models only appear because Polytone is not in the dev runtime.

## Gotchas

- **Expression syntax.** On 1.21.1, Scripting Expressions (`o.age`, `g.rain`,
  `r.rand()`) replace the old uppercase Math Expressions (`AGE`, `RAIN`,
  `rand()`). The old form still loads but is deprecated. All of our
  `custom_particles/*.json` use the old form. Use the new form for new files;
  the Scripting-Expressions wiki page has a conversion table.
- **One colormap per block.** When two modifiers target the same block,
  Polytone 1.21.1 replaces the whole colormap rather than merging by tint
  index, and the `priority` sort is commented out
  (`BlockPropertiesManager`). So per-tint-index colours need one indexed
  colormap, e.g. `"colormap": {"0": "westerosblocks:grass", "1": "westerosblocks:foliage_oak"}`.
- **Missing targets fail the file.** A `targets` id that does not exist outside
  the `minecraft` namespace is an error. Every block listed in an
  `item_modifiers` file must have an item.
- **Wiki vs 1.21.1 code.** The wiki says still and flowing fluids must be
  targeted separately. In 1.21.1 a modifier on `water` also covers
  `flowing_water`. The wiki says `kill_when_still` defaults to `true`. In 1.21.1
  it defaults to `false`.
- **Unknown fields are ignored.** `wildfire.json` sets `"lit": true`, which is
  not a Polytone field. `light_level: 15` is what makes it bright.
- **Sodium.** The Colormaps page warns that some Sodium versions render some
  colormaps wrong. Check this first when tints look off in the pack but the
  files are right.

## Known issues found while writing this (not fixed)

- `westerosblocks:wildfire` is defined in `custom_particles/` and the `wildfire`
  block has `"particle": "wildfire"`, but no block modifier spawns it and no
  Java code reads that field. The particle never appears.
- `butterfly.json` sets `"limit": 1`. That limits it to one butterfly particle on
  the whole client at a time. Check whether that is intended.
- `colormaps/brick.png`, `lapisore.png` and `mossy_small_stone_brick.png` are not
  referenced by any `colorMult`.
