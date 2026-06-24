# Utility Mobs: Redux

A 1.12.2 port of **Utility Mobs** by FatherToast. Adds simple utility mobs in the
spirit of the vanilla snow and iron golems: combat golems, block golems (chest,
furnace, anvil, jukebox, workbench, etc.), turret golems, and colossal golems.

- **Minecraft:** 1.12.2
- **Forge:** 14.23.5.2860
- **Mod version:** 3.1.1
- **Required dependency:** [Patchouli](https://github.com/VazkiiMods/Patchouli) (ships the in-game guide book)

## Features

- **Combat golems** - iron, armor, gilded, stone, obsidian, scarecrow, bound soul, steam, melon
- **Block golems** - chest, trapped chest, ender chest, furnace, anvil, jukebox, workbench, lantern
- **Turrets** - arrow, fire, fireball, ghast, snow, brick, stone, shotgun, sniper, gatling, volley, killer, obsidian
- **Colossal golems** - armor, obsidian, stone (rideable giants built from a single block type)
- **In-game guide book** (Patchouli) with build guides, stats, and upgrade docs
- **Target Book** - per-player targeting filters (hostile / passive / neutral toggles, target modes)
- **`/umsummon`** command for batch-spawning golems/turrets/colossi (op level 2)
- Extensive config GUI for balancing and behavior

## Building

```bash
./gradlew build
```

The built jar lands in `build/libs/`.

A deobfuscated Patchouli jar is bundled in `libs/` for compilation. At runtime,
install Patchouli as a normal mod in your `mods/` folder.

## Installing

Drop the release jar into your Minecraft 1.12.2 `mods/` folder alongside
Patchouli. The guide book is granted automatically on first join (configurable).

## Credits

Original 1.7.10 mod by **FatherToast**. This is an unofficial 1.12.2 port.

## License

GPL-3.0. See [LICENSE](LICENSE). The original mod is © FatherToast, released
under GPL-3.0; this port inherits that license.
