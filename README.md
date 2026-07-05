<div align="center">

  <h1>Utility Mobs: Redux (1.12.2 Forge)</h1>

  <p>Combat golems, block golems, turrets, and colossal golems for Minecraft 1.12.2.</p>

</div>

A 1.12.2 port and extension of [FatherToast's Utility Mobs](https://www.curseforge.com/minecraft/mc-mods/utility-mobs),
originally written for Minecraft 1.7.10. Everything from the original is present, Redux adds a detailed
in-game guide book, a full config GUI, right-click healing, a batch-spawn command, and a pile of
balance and performance adjustments.

## Features

- **Combat golems.** Iron, armor, gilded, stone, obsidian, scarecrow, bound soul, steam, and melon golems that roam and hunt hostiles, aimed with a Target Book.
- **Block golems.** Chest, trapped chest, ender chest, furnace, anvil, jukebox, workbench, and jack o'lantern golems - portable, sittable utility mobs that store, smelt, craft, and light.
- **Turrets.** Arrow, fire, fireball, ghast, snow, brick, stone, shotgun, sniper, gatling, volley, killer, and obsidian turrets, each with its own range, damage, and projectile profile and a slot for upgrades.
- **Colossal golems.** Armor, obsidian, and stone colossi - giant rideable golems built from a single block type. Left-click to swing their arms; they soak damage for the rider.
- **Turret upgrades.** Fire, explosive, fire-explosive, killer, feather, slow, sight, poison, and egg upgrades swap onto a turret and change how it shoots.
- **Target Book.** A per-player targeting filter - toggle hostile / passive / neutral, and pick nearest, farthest, strongest, or weakest target modes. Multiplayer-safe, keyed to the owner.
- **Attack whitelist / blacklist.** Force golems and turrets to always attack, or never attack, any mob - vanilla or modded - via config or the `/umwhitelist` and `/umblacklist` commands (just look at a mob to add it). Modded hostiles are attacked out of the box.
- **In-game guide book.** A Patchouli book with build guides (live multiblock projections), stat pages, and upgrade docs. Granted automatically on first join.
- **Right-click healing.** Right-click a golem with its repair (drop) item to heal it - works on combat, block, and turret golems.
- **`/umsummon` command.** Batch-spawn golems, turrets, or colossi for staged mob battles (`/umsummon <type> <count> [team|hostile]`).
- **Config GUI.** Every behavior and balance value is editable live under Mods, Utility Mobs, Config - ammo requirements, drop chances, target scan tuning, collision caps, build toggles per mob, and more.
- **Big-army performance.** Config-tunable target-scan caps, collision push-caps and density-disable, activation-range gating, and budgeted so a parked army costs almost nothing.

## Requirements

- Minecraft 1.12.2
- Minecraft Forge 14.23.5.2860 or newer
- [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli)

## Installation

1. Install Minecraft Forge for 1.12.2.
2. Drop Patchouli and `utilitymobs-3.3.0.jar` into your `mods` folder.
3. Launch the game. The guide book is granted automatically on first join (configurable).

## Building

```
./gradlew build
```

The built jar lands in `build/libs/`. A deobfuscated Patchouli jar is bundled in `libs/` for compilation; at runtime, install Patchouli as a normal mod.

## Credits

- Original Utility Mobs mod by **FatherToast** ([CurseForge](https://www.curseforge.com/minecraft/mc-mods/utility-mobs)).
- 1.12.2 Forge port and Redux additions by **Xy**.

## License

Licensed under the **GNU General Public License v3.0**, the same license as the original mod.
See [LICENSE](LICENSE).
