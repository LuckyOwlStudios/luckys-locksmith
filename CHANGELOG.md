# Changelog

All notable changes to Lucky's Locksmith are listed here.
Every pull request adds its entry under **Work In Progress**. When a version is released,
that section becomes the new version (with a row in the table below), and a new empty
Work In Progress section is started.

# Navigation
(Click on version number to jump to its changelog.)

| Version | Release Date | Type |
|---------|--------------|------|
| [1.1.1](#111-work-in-progress) | `TBD` | Release |
| [1.1.0](#110) | `11/23/2026` | Release |
| [1.0.3](#103) | `04/15/2026` | Release |
| [1.0.0](#100) | `07/08/2025` | Release |

# 1.1.1 (Work In Progress)

**Added:**
- **Lock Pick** and a **lock picking mini game**. Craft a lock pick from 2 iron nuggets over a stick, then right-click another player's locked container with it. A cutaway lock opens with spring-loaded pins bobbing in their channels. Steer the pick under a pin with A and D (or the arrow keys) and press space or click while that pin is on the green shear line to set it. Set every pin and the lock is picked and removed.
  - The more experience levels the lock's owner has, the harder it is: more pins (3, then 4 from level 15, then 5 from level 30), a thinner shear line and faster pins. At 30 levels or more the shear line is under a pixel thick, so it is nearly impossible. An owner who is offline counts as level 0.
  - Starting an attempt uses 1 of the pick's 8 durability. Missing, or closing the screen early, snaps the pick.
  - Only player locks can be picked. Golden and trial locks still need their own keys.
  - The lock pick uses a placeholder texture for now.
- **One-click build** for developers: `build.bat` / `build.sh` (or `./gradlew buildRelease`) builds both the Fabric and NeoForge jars and copies them into `release/<version>/`. IntelliJ also gets a **Build Release** run button in the top-right run dropdown.

**Changed:**

**Fixed:**

# 1.1.0

**Added:**
- **Accessories compatibility** (optional). With the [Accessories](https://www.curseforge.com/minecraft/mc-mods/accessories) mod installed, keys can be worn in a dedicated key slot on both NeoForge and Fabric, and a worn key opens matching locks without being held. This also gives Fabric its first worn key slot. ([#24](https://github.com/luckyowlstudios/luckys-locksmith/pull/24))
- **Jeweled Trapped Chest**, the trapped version of the Jeweled Chest. Craft it from a Jeweled Chest and a tripwire hook. ([#25](https://github.com/luckyowlstudios/luckys-locksmith/pull/25))
- **Jeweled Chest**, a gold and diamond chest with dyeable fabric trim (purple until dyed), double chest support and lock support like the Golden Chest. Craft it from a Golden Chest surrounded by 4 diamonds. ([#25](https://github.com/luckyowlstudios/luckys-locksmith/pull/25))
- **Fabric support!** Locksmith now runs on both NeoForge and Fabric for Minecraft 1.21.1. ([#5](https://github.com/luckyowlstudios/luckys-locksmith/pull/5))
  - On Fabric, keys work from your hand or inventory, or from a worn key slot with the optional Accessories mod.
- **Dungeon Slime**, a new mob and the new source of golden keys. It's a gray, see-through
  slime with a golden key floating inside.
  - Spawns in the Overworld below Y 0 in full darkness (not on Peaceful, in mushroom fields
    or in the Deep Dark).
  - Always spawns as a medium slime and drops exactly one golden key. It splits into small
    slimes on death, which drop slimeballs instead of keys.
  - Has its own spawn egg.
  - Also spawns commonly inside strongholds, even above Y 0 (it still needs darkness).
- Some Nether fortress chests now generate with a **golden lock** (25% chance per chest). Bring
  a golden key! Unopened fortress chests in existing worlds can get locked too.
  - On NeoForge, the chance can be changed with the `fortressChestLockChance` config option
    (set it to 0 to turn this off).
- **Locks on doors and trapdoors!** Lock any door or trapdoor (including iron, copper and modded
  ones) with a key, golden lock or trial lock, just like a chest. Door locks and their visible
  padlocks are based on the contribution by [KevoMartinez256](https://github.com/KevoMartinez256)
  (Don Kevo). Thank you! ([#2](https://github.com/luckyowlstudios/luckys-locksmith/pull/2), [#15](https://github.com/luckyowlstudios/luckys-locksmith/pull/15), [#18](https://github.com/luckyowlstudios/luckys-locksmith/pull/18))
  - A padlock is drawn on both sides of a locked door and follows it as it swings. Trapdoors don't
    show a padlock yet.
  - A locked door or trapdoor stays shut against redstone, villagers, zombies, pistons and wind
    charges until it's opened with the right key.
  - Locked doors can't be blown up or broken by other players, and neither can the block holding
    them up. The owner, or anyone with the key, can still break them.
  - A door's two halves share one lock, and a copper door keeps its lock as it weathers.
  - Key codes and owners stay on the server; players only learn which doors are locked.

**Changed:**
- **Golden keys can no longer be crafted.** Find them on Dungeon Slimes or in Nether fortress
  chests.
- Piglin brutes no longer drop golden keys.
- Nether fortress chests now have a 15% chance to hold a golden key (it used to be every chest).
- Golden keys and trial keys now stack up to 16.
- New recipes for the **iron chest** and **golden chest**: a chest surrounded by 4 iron or gold
  ingots (corners empty). They used to need 4 blocks and 4 wool.
- Reworked **iron chest** and **golden chest** textures. Iron chests show natural wood until
  dyed, and golden chests have a fabric trim that takes the dye color.
- Curios is now optional on NeoForge and listed as an optional dependency. Install it to wear your key in a Curios slot. ([#16](https://github.com/luckyowlstudios/luckys-locksmith/pull/16))
- The "is Locked!" message now shows in red. ([#12](https://github.com/luckyowlstudios/luckys-locksmith/pull/12))

**Fixed:**
- Locking one half of a double chest no longer changes the other half's dye color. ([#11](https://github.com/luckyowlstudios/luckys-locksmith/pull/11))
- Hoppers and hopper minecarts can no longer pull items out of a locked chest or other locked container. ([#23](https://github.com/luckyowlstudios/luckys-locksmith/pull/23))

# 1.0.3

**Fixed:**
- You can now break your own locked chest.
- An already locked chest can no longer be locked again.

**Added:**
- Key tooltip explaining how to duplicate keys.

# 1.0.0

**Added:**
- First release: locks, keys, iron chests and golden chests.
