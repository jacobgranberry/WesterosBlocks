#!/usr/bin/env python3
"""Check that every texture reference in WesterosBlocks points at a real file.

Run from the repository root, after `./gradlew runDatagen` so that the
generated models in src/main/generated/ are current:

    python scripts/check_textures.py              # report problems, exit 1 if any
    python scripts/check_textures.py --unused     # also list textures nothing references

Texture names are written differently in each system. This script resolves
them the way the game and the client mods do:

- Block definitions (definitions/WesterosBlocks.json): a bare path means
  westerosblocks:block/<path>; `ns:path` is used as written.
- `customItemTexture: true` blocks: westerosblocks:item/<blockName>.
- Models (hand-made and generated): a bare path means minecraft:<path>.
- Particle JSON (assets/*/particles): `ns:name` means ns:particle/name.
- WesterosContinuity CTM (assets/*/optifine/ctm/**.properties): the rules in
  docs/connected-textures.md ("Path rules").
- Polytone: `colorMult`/`colorMults` name a colormap in
  assets/westerosblocks/polytone/colormaps/.
- Java: string literals "textures/....png" (AzureLib renderers, items).

Errors (exit code 1):
  missing-texture   a reference with no PNG behind it
  missing-ctm-tile  a CTM `tiles=` entry with no PNG behind it
  missing-colormap  a colorMult with no Polytone colormap
  orphan-mcmeta     a .mcmeta file with no PNG, or not named <texture>.png.mcmeta

Known, accepted problems are listed in scripts/check_textures_allow.txt, one
`<kind> <id>` per line with a reason after `#`. A listed problem that no
longer occurs is reported as stale so the list stays honest.

Uses only the Python standard library.
"""
import argparse
import json
import os
import re
import sys
from collections import defaultdict

ASSETS = 'src/main/resources/assets'
GENERATED = 'src/main/generated/assets'
DEFINITIONS = 'src/main/resources/definitions'
JAVA = 'src/main/java'
ALLOW = 'scripts/check_textures_allow.txt'

TEXTURE_KEYS = ('textures', 'randomTextures', 'overlayTextures', 'sideTextures',
                'altTextures', 'altRandomTextures', 'itemTexture')


class Refs:
    def __init__(self):
        self.sources = defaultdict(set)  # sprite id -> {source description}

    def add(self, sprite, source):
        if sprite.endswith('.png'):
            sprite = sprite[:-4]
        self.sources[sprite].add(source)


def sprite_of(value, default_ns, default_prefix=''):
    """'a/b' -> 'default_ns:<default_prefix>a/b'; 'ns:a/b' -> 'ns:a/b'."""
    if ':' in value:
        return value
    return f'{default_ns}:{default_prefix}{value}'


def walk_files(root, suffix):
    for dirpath, _, files in os.walk(root):
        for name in files:
            if name.endswith(suffix):
                yield os.path.join(dirpath, name)


def namespaces(root):
    return sorted(d for d in os.listdir(root) if os.path.isdir(os.path.join(root, d))) if os.path.isdir(root) else []


# ---------------------------------------------------------------- definitions

def scan_definitions(refs, colormap_refs):
    defs = json.load(open(os.path.join(DEFINITIONS, 'WesterosBlocks.json'), encoding='utf-8'))

    def walk(node, in_textures, where):
        if isinstance(node, dict):
            for key, value in node.items():
                walk(value, in_textures or key in TEXTURE_KEYS, where)
        elif isinstance(node, list):
            for value in node:
                walk(value, in_textures, where)
        elif in_textures and isinstance(node, str) and node:
            refs.add(sprite_of(node, 'westerosblocks', 'block/'), f'definitions ({where})')

    for block in defs.get('blocks', []):
        name = block.get('blockName', '?')
        walk(block, False, name)
        if block.get('customItemTexture'):
            refs.add(f'westerosblocks:item/{name}', f'definitions ({name}, customItemTexture)')
        for mult in [block.get('colorMult')] + list(block.get('colorMults') or []):
            if isinstance(mult, str) and not mult.startswith('#'):
                colormap_refs[colormap_id(mult)].add(f'definitions ({name})')
        for state in block.get('states') or []:
            mult = state.get('colorMult')
            if isinstance(mult, str) and not mult.startswith('#'):
                colormap_refs[colormap_id(mult)].add(f'definitions ({name})')
    for blockset in defs.get('blockSets', []):
        name = blockset.get('baseBlockName', '?')
        walk(blockset, False, name)
        mult = blockset.get('colorMult')
        if isinstance(mult, str) and not mult.startswith('#'):
            colormap_refs[colormap_id(mult)].add(f'definitions ({name})')

    color_maps = os.path.join(DEFINITIONS, 'color_maps.json')
    if os.path.exists(color_maps):
        for entry in json.load(open(color_maps, encoding='utf-8')).get('colorMaps', []):
            mult = entry.get('colorMult')
            if isinstance(mult, str) and not mult.startswith('#'):
                colormap_refs[colormap_id(mult)].add('color_maps.json')


def colormap_id(value):
    """Mirror ModPolytoneProvider.extractColormapId: last path segment, namespace dropped."""
    value = value.split(':', 1)[-1]
    return value.rsplit('/', 1)[-1]


# --------------------------------------------------------------------- models

def scan_models(refs, root, label):
    for ns in namespaces(root):
        for path in walk_files(os.path.join(root, ns, 'models'), '.json'):
            try:
                model = json.load(open(path, encoding='utf-8'))
            except (ValueError, OSError) as e:
                print(f'warning: cannot read {path}: {e}')
                continue
            textures = model.get('textures') or {}
            rel = os.path.relpath(path, root)
            for value in textures.values():
                if isinstance(value, str) and value and not value.startswith('#'):
                    refs.add(sprite_of(value, 'minecraft'), f'{label} model {rel}')


def scan_particles(refs):
    for ns in namespaces(ASSETS):
        for path in walk_files(os.path.join(ASSETS, ns, 'particles'), '.json'):
            rel = os.path.relpath(path, ASSETS)
            for value in json.load(open(path, encoding='utf-8')).get('textures', []):
                pns, name = value.split(':', 1) if ':' in value else ('minecraft', value)
                refs.add(f'{pns}:particle/{name}', f'particle {rel}')


def scan_java(refs):
    literal = re.compile(r'"textures/([a-z0-9_/.\-]+?\.png)"')
    for path in walk_files(JAVA, '.java'):
        source = open(path, encoding='utf-8', errors='replace').read()
        for match in literal.finditer(source):
            refs.add(f'westerosblocks:{match.group(1)}', f'java {os.path.relpath(path, JAVA)}')


# ------------------------------------------------------------------------ CTM

def parse_properties(path):
    props = {}
    for line in open(path, encoding='utf-8', errors='replace'):
        line = line.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        key, value = line.split('=', 1)
        props[key.strip()] = value.strip()
    return props


def scan_ctm(refs, problems):
    """Adds matchTiles sprites to refs; returns the set of CTM tile PNGs referenced."""
    used_tiles = set()
    for ns in namespaces(ASSETS):
        ctm_root = os.path.join(ASSETS, ns, 'optifine')
        for path in walk_files(os.path.join(ctm_root, 'ctm'), '.properties'):
            props = parse_properties(path)
            folder = os.path.dirname(path)
            rel = os.path.relpath(path, ASSETS)
            for key in ('matchTiles', 'connectTiles'):
                for tile in props.get(key, '').split():
                    if tile.endswith('.png'):
                        tile = tile[:-4]
                    if tile.startswith('./'):
                        used_tiles.add(os.path.normpath(os.path.join(folder, tile[2:] + '.png')))
                    elif tile.startswith('optifine/'):
                        used_tiles.add(os.path.normpath(os.path.join(ASSETS, ns, tile + '.png')))
                    elif ':' in tile:
                        tns, tpath = tile.split(':', 1)
                        if tpath.startswith('textures/'):
                            tpath = tpath[len('textures/'):]
                        refs.add(f'{tns}:{tpath}', f'ctm {rel}')
                    else:
                        refs.add(f'minecraft:{tile}' if '/' in tile else f'minecraft:block/{tile}', f'ctm {rel}')
            for tile in re.split(r'[\s,]+', props.get('tiles', '')):
                if not tile or tile.startswith('<'):
                    continue
                if ':' in tile:
                    tns, tpath = tile.split(':', 1)
                    if tpath.startswith('optifine/'):
                        used_tiles.add(os.path.normpath(os.path.join(ASSETS, tns, tpath + '.png')))
                        continue
                    if tpath.startswith('textures/'):
                        tpath = tpath[len('textures/'):]
                    elif '/' not in tpath:
                        tpath = 'block/' + tpath
                    refs.add(f'{tns}:{tpath}', f'ctm tiles {rel}')
                    continue
                if tile.endswith('.png'):
                    tile = tile[:-4]
                ranged = re.fullmatch(r'(.*?)(\d+)-(\d+)', tile)
                names = ([f'{ranged.group(1)}{i}' for i in range(int(ranged.group(2)), int(ranged.group(3)) + 1)]
                         if ranged else [tile])
                for name in names:
                    tile_path = os.path.normpath(os.path.join(folder, name + '.png'))
                    used_tiles.add(tile_path)
                    if not os.path.exists(tile_path):
                        problems.append(('missing-ctm-tile', os.path.relpath(tile_path, ASSETS), rel))
    return used_tiles


# ------------------------------------------------------------------ inventory

def inventory():
    sprites = {}
    for ns in namespaces(ASSETS):
        root = os.path.join(ASSETS, ns, 'textures')
        for path in walk_files(root, '.png'):
            sprites[f'{ns}:{os.path.relpath(path, root)[:-4].replace(os.sep, "/")}'] = path
    ctm_tiles = set()
    for ns in namespaces(ASSETS):
        for path in walk_files(os.path.join(ASSETS, ns, 'optifine'), '.png'):
            ctm_tiles.add(os.path.normpath(path))
    colormaps = set()
    for path in walk_files(os.path.join(ASSETS, 'westerosblocks', 'polytone', 'colormaps'), '.png'):
        colormaps.add(os.path.basename(path)[:-4])
    return sprites, ctm_tiles, colormaps


def check_mcmeta(problems):
    for path in walk_files(ASSETS, '.mcmeta'):
        rel = os.path.relpath(path, ASSETS)
        if not path.endswith('.png.mcmeta'):
            problems.append(('orphan-mcmeta', rel, 'must be named <texture>.png.mcmeta'))
        elif not os.path.exists(path[:-len('.mcmeta')]):
            problems.append(('orphan-mcmeta', rel, 'no PNG next to it'))


def load_allow():
    allowed = {}
    if os.path.exists(ALLOW):
        for line in open(ALLOW, encoding='utf-8'):
            line = line.split('#', 1)[0].strip()
            if line:
                kind, ident = line.split(None, 1)
                allowed[(kind, ident.strip())] = False
    return allowed


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n\n')[0])
    parser.add_argument('--unused', action='store_true', help='also list textures and CTM tiles nothing references')
    args = parser.parse_args()
    if not os.path.isdir(ASSETS):
        sys.exit('Run this from the repository root.')

    refs = Refs()
    colormap_refs = defaultdict(set)
    problems = []

    scan_definitions(refs, colormap_refs)
    scan_models(refs, ASSETS, 'hand-made')
    if os.path.isdir(GENERATED):
        scan_models(refs, GENERATED, 'generated')
    else:
        print('warning: src/main/generated is missing; run ./gradlew runDatagen first. '
              'Textures used only by generated models will look unused.')
    scan_particles(refs)
    scan_java(refs)
    used_tiles = scan_ctm(refs, problems)
    check_mcmeta(problems)
    sprites, ctm_tiles, colormaps = inventory()

    for sprite, sources in sorted(refs.sources.items()):
        if sprite not in sprites and not sprite.startswith('minecraft:'):
            problems.append(('missing-texture', sprite, sorted(sources)[0]))
        elif sprite not in sprites and any(not s.startswith('hand-made model assets/minecraft') for s in sources):
            # minecraft: sprites may be vanilla files that are not in this repo. Only flag
            # paths vanilla never uses: anything below a subfolder of block/ or item/.
            if re.match(r'minecraft:(block|item)/[^/]+/', sprite):
                problems.append(('missing-texture', sprite, sorted(sources)[0]))
    for cid, sources in sorted(colormap_refs.items()):
        if cid not in colormaps:
            problems.append(('missing-colormap', cid, sorted(sources)[0]))

    allowed = load_allow()
    errors = []
    for kind, ident, where in problems:
        if (kind, ident) in allowed:
            allowed[(kind, ident)] = True
        else:
            errors.append((kind, ident, where))

    for kind, ident, where in sorted(set(errors)):
        print(f'ERROR {kind:16} {ident}   <- {where}')
    for (kind, ident), seen in sorted(allowed.items()):
        if not seen:
            print(f'stale allow entry: {kind} {ident} (no longer occurs; remove it from {ALLOW})')

    if args.unused:
        unused = sorted(s for s in sprites if s not in refs.sources)
        print(f'\n{len(unused)} textures nothing in this repo references '
              f'(minecraft: ones may be vanilla overrides that vanilla models use):')
        for sprite in unused:
            print(f'  unused-texture  {sprite}')
        unused_tiles = sorted(ctm_tiles - used_tiles)
        print(f'\n{len(unused_tiles)} CTM tile PNGs no .properties file names:')
        for tile in unused_tiles:
            print(f'  unused-ctm-tile {os.path.relpath(tile, ASSETS)}')
        unused_maps = sorted(colormaps - set(colormap_refs))
        print(f'\n{len(unused_maps)} Polytone colormaps no colorMult names:')
        for name in unused_maps:
            print(f'  unused-colormap {name}')

    print(f'\n{len(sprites)} textures, {len(ctm_tiles)} CTM tiles, {len(refs.sources)} referenced sprites, '
          f'{len(errors)} errors.')
    sys.exit(1 if errors else 0)


if __name__ == '__main__':
    main()
