# Vendored agent skills

These Claude Code skills are copied from
[Jahrome907/minecraft-agent-skills](https://github.com/Jahrome907/minecraft-agent-skills)
at commit `22d61ce6b7a0c5f215008824aa6b94ecc73f2a43` (2026-10-30), under the MIT
license in `LICENSE-minecraft-agent-skills`.

Only the skills relevant to a Fabric block mod are included:

- `minecraft-modding`: loader code, registries, datagen wiring
- `minecraft-resource-pack`: blockstates, models, textures, lang files
- `minecraft-datapack`: loot tables, tags, recipes
- `minecraft-testing`: JUnit and Fabric Game Tests

The skills target Minecraft 26.x first and keep 1.21.x examples. While this
branch is on 1.21.1, check advice against the 1.21.1 APIs (Yarn names, Loom
remapping, Java 21) rather than copying 26.x snippets.

To update, re-copy these folders from a newer commit of the upstream repo and
bump the commit hash above.
