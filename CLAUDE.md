# CLAUDE.md

All project guidance lives in AGENTS.md, which is shared with other agents and
imported below. Its rules, especially the world-compatibility rule, apply in
full. This file only adds Claude Code specifics.

@AGENTS.md

## Claude Code specifics

### minecraft-dev MCP server

`.mcp.json` registers `minecraft-dev` (`@mcdxai/minecraft-dev-mcp`, pinned to
1.3.0). It searches decompiled Minecraft source, looks up Yarn and Mojang
mappings, and checks Mixins and access wideners. Use it to confirm real 1.21.1
Yarn class, method and field names instead of guessing, especially when porting
1.18.2 Forge code (which used Mojang names). Ask for version `1.21.1` with Yarn
mappings.

The config launches it with `cmd /c npx`, so it starts on Windows (Jacob's
machine). In a Linux or macOS sandbox it will not start as configured; work
without it rather than editing `.mcp.json`.

### Vendored skills

`.claude/skills/` holds skills copied from minecraft-agent-skills (see
`.claude/skills/README.md` for source and license):

- `minecraft-modding`: Fabric code, registries, datagen wiring
- `minecraft-resource-pack`: blockstates, models, textures, lang
- `minecraft-datapack`: loot tables, tags, recipes
- `minecraft-testing`: JUnit and Fabric Game Tests

They target Minecraft 26.x first. Check any snippet against 1.21.1 and Yarn
before using it. Only `.claude/skills/` is committed; other `.claude/` files
(local settings) are gitignored.
