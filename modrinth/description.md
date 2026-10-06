Automatically rerolls librarian trades until the villager offers the enchanted book you want. Client-side only, so it works on any server, vanilla or Paper, without the server installing anything.

This is a Minecraft 26.3 continuation of [Villager Reroller by joplayx](https://modrinth.com/mod/villager-reroller), which stopped at 26.2. The reroll loop and the original idea are theirs. This version adds survival fixes and a handful of features. If you're on 26.1.2 or 26.2, use the original.

## How it works

You stand next to an unemployed villager with lecterns and an axe. The mod places the lectern, waits for the villager to become a librarian, opens the trade screen and reads every enchanted book on offer. No match? It closes the screen, breaks the lectern and tries again. When a book matches your list (enchantment, minimum level, maximum emerald price), it stops and plays a sound.

You can hunt for several books at once. Whichever matches first wins and drops off your list, so the next villager only looks for what's left.

## What this version adds

- **Minecraft 26.3** support.
- **Lecterns anywhere in your inventory.** Hotbar, offhand or main inventory all work. The original only checked the hotbar.
- **Walks to pick up the lectern.** If the broken lectern lands out of reach, the mod sneaks over, picks it up and walks back to where you stood.
- **Pick the villager.** Look at one and type `/reroll villager`. It gets an outline only you can see, which helps in a crowded trading hall.
- **`/reroll` commands** for everything: targets, lectern spot, HUD, timings. They run on your client and are never sent to the server. Useful when another mod hides the Mod Menu button.
- **Resizable HUD.** Scale 0.5 to 2.0, six screen positions, and a one-line compact mode.
- **Bug fixes:** the set-position key now picks the empty space above the block you aim at, stale trades from the previous attempt are never re-read, and it stops before your axe breaks.

## Quick start

1. Hold lecterns and an axe, and stand next to an unemployed villager.
2. Aim at the floor where the lectern goes and press **L** (or `/reroll pos`).
3. Add a target: `/reroll add mending 1 20` (Mending, any level, 20 emeralds max).
4. Press **J** or `/reroll start`. **K** or `/reroll stop` stops it.

Type `/reroll` in chat to see every command. `/reroll settings` shows your current values next to the defaults.

## Good to know

- The pickup walk goes in a straight line. It won't jump or path around blocks, and gives up after 10 seconds if something is in the way.
- The walk turns your character to face the dropped lectern. If a server's anti-cheat complains, turn it off with `/reroll pickup false`.
- Check your server's rules before using automation mods.

## Requirements

Fabric Loader 0.19.5+, Fabric API, YetAnotherConfigLib and Mod Menu.

## Links

Source code, full command list and default settings: [GitHub](https://github.com/1REDfriend/villager-reroll-26.3). Bug reports go to [GitHub Issues](https://github.com/1REDfriend/villager-reroll-26.3/issues).
