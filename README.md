<div align="center">

  <h1>Utility Mobs: Redux (1.20.1 Forge)</h1>

  <p>Combat golems, block golems, turrets, and colossal golems for Minecraft 1.20.1.</p>

</div>

A 1.20.1 Forge port of [Utility Mobs: Redux](https://github.com/haunterdev/UtilityMobs/tree/1.12.2),
which is itself a port and extension of [FatherToast's Utility Mobs](https://www.curseforge.com/minecraft/mc-mods/utility-mobs).

Looking for the 1.12.2 version? Its here -> [`1.12.2` branch](https://github.com/haunterdev/UtilityMobs/tree/1.12.2).

## Features

- **Combat golems.** Iron, snow, armor, gilded, stone, large stone, obsidian, scarecrow, bound soul, steam, melon, and stack golems that roam and hunt hostiles, aimed with a Target Book.
- **Block golems.** Chest, trapped chest, ender chest, furnace, anvil, jukebox, workbench, and jack o'lantern golems: portable, sittable utility mobs that store, smelt, craft, and light.
- **Turrets.** Stone, brick, fire, fireball, ghast, snow, shotgun, sniper, gatling, volley, killer, and obsidian turrets, each with its own range, damage, and projectile profile and a slot for upgrades.
- **Colossal golems.** Armor, obsidian, and stone colossi: giant rideable golems built from a single block type. Left-click to swing their arms; they soak damage for the rider.
- **Turret upgrades.** Fire, explosive, fire-explosive, killer, slow, sight, poison, and egg upgrades swap onto a turret and change how it shoots. The feather upgrade instead frees a turret to walk rather than stay rooted.
- **Target Book.** A per-player targeting filter: toggle hostile / passive / neutral, and pick nearest, farthest, strongest, or weakest target modes. Multiplayer-safe, keyed to the owner.
- **Attack whitelist / blacklist.** Force golems and turrets to always attack, or never attack, any mob, vanilla or modded, via config or the `/umwhitelist` and `/umblacklist` commands (just look at a mob to add it). Modded hostiles are attacked out of the box.
- **In-game guide book.** A Patchouli book with build guides (live multiblock projections), stat pages, and upgrade docs. Granted automatically on first join.
- **Right-click healing.** Right-click a golem with its repair (drop) item to heal it. Works on combat, block, and turret golems.
- **`/umsummon` command.** Batch-spawn golems, turrets, or colossi for staged mob battles (`/umsummon <type> <count> [team|hostile]`).
- **Config screen.** Every behavior and balance value is editable live under Mods, Utility Mobs, Config: ammo requirements, drop chances, target scan tuning, collision caps, build toggles per mob, and more.
- **Big-army performance.** Config-tunable target-scan caps, collision push-caps and density-disable, activation-range gating, and budgeted so a parked army costs almost nothing.
- **Admin Sword.** A creative-tab testing tool that kills anything in one hit, for clearing golems and colossi during setup.

## Requirements

- Minecraft 1.20.1
- Minecraft Forge 47.0.0 or newer
- [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli) (optional)

## Installation

1. Install Minecraft Forge for 1.20.1.
2. Drop `utilitymobs-3.3.0.jar` into your `mods` folder, plus Patchouli if you want the guide book.
3. Launch the game. The guide book is granted automatically on first join (configurable).

## Building

```
./gradlew build
```

The built jar ends up in `build/libs/`. Patchouli is pulled from the BlameJared maven at compile time,
so no jar needs to be vendored.

## Credits

- Original Utility Mobs mod by **FatherToast** ([CurseForge](https://www.curseforge.com/minecraft/mc-mods/utility-mobs)).
- Redux additions and the 1.12.2 and 1.20.1 Forge ports by **Xy**.

## License

Licensed under the **GNU General Public License v3.0**, the same license as the original mod.
See [LICENSE](LICENSE).
