# AzureLib in WesterosBlocks

Reference for agents and contributors who touch the big door, the shields, or
anything under `assets/westerosblocks/geo/` or `animations/`. AzureLib renders
Bedrock-format (`.geo.json`) models with optional keyframe or in-code
animation. It is a fork of GeckoLib with its own `Az*` API.

Unlike Polytone and WesterosContinuity, AzureLib is a **real, required
dependency**: it is `modImplementation` in `build.gradle`, `depends` in
`fabric.mod.json`, and it is on the classpath in `runClient`. Servers need it
too, because `WCBigDoorBlockEntity` (common code) calls `AzEasingUtil`.

## Where to look things up

- Docs: <https://moddedmc.wiki/en/project/azurelib/latest/docs>. The "latest"
  docs are written from the `1.21.1` branch of
  <https://github.com/AzureDoom/AzureLib>, and 3.2.7 is the newest 1.21.1
  release, so the docs describe the version we target.
- Version: `azurelib_version` in `gradle.properties`. The base branch still
  says **3.1.10**; PR #4 bumps it to **3.2.7**. Some features in the docs
  arrived after 3.1.10 (see "Version differences" below). Check
  `gradle.properties` on your branch before using one.
- Source: branch `1.21.1` of the AzureLib repo, `changelog.md` for the current
  release. On Jacob's PC Loom keeps remapped sources jars under
  `.gradle/loom-cache/remapped_mods/remapped/mod/azure/azurelib/`; read those
  when the docs are unclear. The source wins over the docs.
- Docs pages that matter here: Blocks Entities > Animating Block Entities;
  Items > Creating Static Items; Animating Topics > Block AzAnimator Basics and
  Animating Bones In-Code; Rendering Topics > AzRendererConfigs 101; Blockbench
  Plugin > Exporting.
- Packages are `mod.azure.azurelib.common.*` (for example
  `common.render.block.AzBlockEntityRenderer`,
  `common.render.item.AzItemRenderer`,
  `common.animation.impl.AzBlockAnimator`). The docs use Mojang names
  (`ResourceLocation`, `PoseStack`); this repo uses Yarn (`Identifier`,
  `MatrixStack`). The docs pages also leave out imports.

## What AzureLib can do (and what we use)

| Feature | Used here? |
|---|---|
| Geo models for block entities (`AzBlockEntityRenderer`) | Yes, the big door |
| Geo models for items (`AzItemRenderer`, static) | Yes, 8 shields |
| Keyframe animations from `animations/*.animation.json`, triggered with `AzCommand` / dispatchers and `AzAnimationController` | No (the door's file is an empty stub) |
| Setting bones in code (`AzAnimator#setCustomAnimations`) | Yes, the door leaves |
| Easing functions (`AzEasingUtil`) | Yes, door swing |
| Entities, armor, armor trims, first-person arms, Molang queries, effect keyframes, render layers (glow, animated textures), IK, blend spaces, sockets, hitboxes, meshes, config API | No |

Reach for AzureLib when a block or item needs something a vanilla JSON model
cannot do: geometry that moves (doors, drawbridges, wheels), a model bigger
than a block driven by one block entity, or an item with a 3D model plus
animation. Do **not** reach for it for a static decorative block, even one with
odd rotation angles: vanilla block models plus Polytone already allow
non-vanilla angles (see [polytone.md](polytone.md)) and are far cheaper, since
every AzureLib block is a block entity rendered every frame.

Rotation note: AzureLib's free bone and cube rotations (any angle, on any axis)
apply only to its own `.geo.json` models. They do nothing for vanilla
`models/block/*.json`; those get extra angles from Polytone.

## What we ship

| Path | What it is |
|---|---|
| `assets/westerosblocks/geo/block/bigdoor.geo.json` | The one big door mesh (bones `root`, `left_door`, `right_door`), 48x48 px = 3x3 blocks, texture 96x96 |
| `assets/westerosblocks/animations/block/bigdoor.animation.json` | Empty stub (`"animations": {}`), on purpose; see below |
| `assets/westerosblocks/textures/block/doors/*.png` | Door textures, picked per block from `textures[0]` in the definition |
| `assets/westerosblocks/geo/item/*_shield.geo.json` | 9 shield meshes (8 used; see Known problems) |
| `assets/westerosblocks/textures/item/*_shield.png` | Shield textures, 64x64 |
| `assets/westerosblocks/models/item/*_shield.json`, `*_shield_blocking.json` | Vanilla item models: `"parent": "builtin/entity"`, `display` transforms per context, and a `blocking` override |

Java:

| Class | Role |
|---|---|
| `block/custom/WCBigDoorBlock` | 3x3 multi-block (`PART` property, 9 parts). Only `BOTTOM_CENTER` has a block entity. `getRenderType` returns `ENTITYBLOCK_ANIMATED`, so the parts have no baked model. `getGeoLocation()` / `getTextureLocation()` feed the renderer. |
| `block/blockentity/custom/WCBigDoorBlockEntity` | Swing state (`SwingDir`, `SwingStartTick` in NBT), 40-tick swing, 90 degrees, quadratic easing. Open/closed itself is the `OPEN` blockstate. |
| `block/blockentity/client/WCBigDoorBlockEntityRenderer` | `extends AzBlockEntityRenderer`; config with model/texture resolvers and `setAnimatorProvider(WCBigDoorBlockAnimator::new)` |
| `block/blockentity/client/WCBigDoorBlockAnimator` | `extends AzBlockAnimator`; no controllers; sets `left_door`/`right_door` `rotY` in `setCustomAnimations` |
| `item/custom/ModShieldItem` | Shield API `CustomShieldItem` that also carries its geo and texture paths |
| `item/client/ModShieldRenderer` | `extends AzItemRenderer`, `AzItemRendererConfig.builder(geo, tex).useNewOffset(true)` |
| `WesterosBlocksClient` | Registers shield renderers with `AzItemRendererRegistry.register(item, supplier)` and big door renderers with `BlockEntityRendererFactories.register` for every `bigdoor` definition |

Registration happens in `onInitializeClient`. On Fabric no `enqueueWork`
wrapper is needed (that advice in the docs is for NeoForge).

## How the big door works

- The swing is computed, not keyframed. Both server and client derive the angle
  from the block entity's swing state and `world.getTime()`, so they cannot
  drift apart. `WCBigDoorBlockEntity.MAX_DOOR_ANGLE_DEGREES` is the source of
  truth for the angle.
- `AzAnimator#getAnimationLocation` is abstract, so the animator must return an
  identifier. With zero controllers the animation file is never read. Keep it
  empty so nobody mistakes stale keyframes for the real animation.
- `setCustomAnimations` runs every frame after controllers. The door sets the
  rotation absolutely (`setRotY(angle)`), which is safe. Never write
  `bone.setRotY(bone.getRotY() + x)`: with no controller resetting the bone
  each frame, the value accumulates and the bone spins away. If a bone has a
  rest rotation in the geo, add it from `bone.getInitialAzSnapshot()`.
- `AzBlockEntityModelRenderer` already centers the model and rotates it by
  `FACING` (N 0, W 90, S 180, E 270 degrees about Y). Do not rotate again in
  the renderer.
- One geo is shared by every big door; per-door looks come from the texture
  only.

## Gotchas

- **X is mirrored.** AzureLib's baker (`AzBuiltinBakedModelFactory`) negates
  Bedrock X for cube origins and bone pivots, and negates X/Y rotations. So the
  geo's `left_door` (x -24..0) renders on the east/right side. Check sides in
  game, not by reading the geo numbers.
- **Rotations are radians** in code (`setRotX/Y/Z`), degrees in the geo file.
- **Bone names must match exactly** (`getBoneOrNull("left_door")`), and the
  baked model can be null on the first frames; null-check both.
- **Item display comes from the vanilla item model.** The shield item models
  use `parent: builtin/entity` and their `display` block positions the geo in
  each context. Blockbench 4.11+ exports with a new offset, which is why
  `useNewOffset(true)` is set; a new shield needs the same.
- **`format_version` in geo files is ignored** by 3.1.10 (the enum knows
  1.12.0, 1.14.0, 1.21.0; anything else parses as null and is never read).
  Our files say 1.21.20 and 1.12.0; both load.
- **Export from Blockbench with the AzureLib Animator plugin** (Blockbench
  Plugin > Exporting in the docs), as an AzureLib/Bedrock model, into
  `geo/block/` or `geo/item/`. Animations go into `animations/`.
- **Cost.** Each AzureLib block is a block entity rendered every frame. Fine
  for a handful of doors, wrong for something builders place by the thousand.
- **Existing worlds.** The hard rule in AGENTS.md applies: do not change the
  big door's `PART`, `OPEN` or `FACING` properties, and do not rename the
  NBT keys `SwingDir` / `SwingStartTick` without handling old data.

## Version differences (3.1.10 vs 3.2.7)

- In the docs but missing from 3.1.10: `boneCache.wasRotationAnimatedThisFrame`
  (and the position/scale variants), polygon meshes and skinned meshes
  (3.2.7, need AzureLib Animator 2.4.0), `poly_mesh` geometry loading.
- Present in both: everything this repo uses (`AzBlockEntityRenderer`,
  `AzBlockAnimator`, `AzItemRenderer`, `useNewOffset`,
  `AzItemRendererRegistry.register(item, supplier)`, `AzEasingUtil.quadratic`,
  `getInitialAzSnapshot`).
- 3.2.7 breaking changes only touch `PolyMesh` / `PolysUnion`, which we do not
  use. Whether the 3.2.7 build still compiles is unverified until PR #4 is
  built on Jacob's machine.
- Doc samples have small errors (the block entity page constructs its
  dispatcher with no argument and puts the example animation under
  `animations/item/`). Follow the source when they disagree.

## Known problems (not fixed here)

- **Stark kite shield is orphaned.** `geo/item/stark_kite_shield.geo.json`,
  `textures/item/stark_kite_shield.png` and both item models exist, but there
  is no `STARK_KITE_SHIELD` in `ModItems` and no renderer registration. Either
  register it or delete the assets.
- **Possible culling of the big door's upper blocks (unverified).** The
  renderer does not override `rendersOutsideBoundingBox`, yet the model spans
  3x3 blocks from a block entity at the bottom centre. Vanilla 1.21.1 only
  renders block entities in visible chunk sections unless that method returns
  true, so a door whose bottom sits just below a section boundary may vanish
  when only its top is on screen. Test in game before changing it.
- **Shield list is duplicated.** The shields are listed once in `ModItems` and
  again in `WesterosBlocksClient`; a shield added to one and not the other
  renders as nothing. (This is how the Stark shield's gap could go unnoticed.)

## Checklist for AzureLib changes

1. Decide whether a vanilla model plus Polytone would do. If yes, skip AzureLib.
2. Model and (if needed) animate in Blockbench with the AzureLib Animator
   plugin; export into `geo/` and `animations/`.
3. Renderer: `AzBlockEntityRenderer` or `AzItemRenderer` with a config builder;
   items get `useNewOffset(true)` and a `builtin/entity` item model with
   `display` transforms.
4. Register the renderer in `WesterosBlocksClient.onInitializeClient`.
5. For in-code motion, set bones absolutely in `setCustomAnimations` and keep
   server/client state derived from the same data.
6. Run `runClient` (AzureLib does load there) and check every facing and every
   display context. Say in the PR that you did.
