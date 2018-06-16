# ChestKeeper

ChestKeeper is a drop-in replacement to VirtualChest, which provides signs and commands to manage virtual chests, or "chest keepers", that allow users to store items.

# Features
* Drop-in replacement to VirtualChest
* /gp commands work like they did with VirtualChest
* Threaded IO reduces lag
* Modernized serialization allows for items to keep their NBT data
* Requires Vault for economy features

# Permissions

ChestKeeper has 10 permission nodes:

**Permission**|**Description**
-----|-----
chestkeeper.use|Allows opening chests from signs
chestkeeper.use.anywhere|Allows opening chests from commands
chestkeeper.use.anyone|Allows opening someone else's chests (which can only occur from a command
chestkeeper.sign.keeper.place|Allows a user to place a [Chest Keeper] sign.
chestkeeper.sign.upgrade.place|Allows a user to place an [Up Chest] sign.
chestkeeper.sign.buy.place|Allows a user to place a [Buy Chest] sign.
chestkeeper.sign.break|Allows a user to break placed [Chest Keeper]
chestkeeper.override|Allows a player to override the maximum number of chests
chestkeeper.updates|Players with this permission will receive warning about plugin updates when they join the server.
chestkeeper.convert|Allows a player to run the VirtualChest data conversion process. 

# Commands

## Specifying Players

If a player is specified, the `chestkeeper.use.anyone` permission is required. ChestKeeper will attempt to match the name provided, and will do so case-insensitively.

**Command**|**Permissions Required**|**Description**
-----|-----|-----
/chest open (chest name/player name:chest name)|chestkeeper.use and chestkeeper.use.anywhere|Opens a chest. If no chest is specified
/chest list (player name)|chestkeeper.use|Lists the names of all your chests. If a player name is specified
/chest buy <normal/large> (name)|chestkeeper.use|Buys a chest of the specified size and optionally names it the specified name. If prices for chests are configured
/chest empty <chest name/player name:chest name>|chestkeeper.use|Removes all items from the specified chest. If a player name is specified
/chest nuke <player name>|chestkeeper.use and chestkeeper.use.anyone|Deleted all of the specified user's chests. This command can be executed from the console. 
/chest delete <chest name/player name:chest name>|chestkeeper.use|Delete the specified chest. If a player name is specified
/chest default <chest name>|chestkeeper.use|Set the specified chest to be the player's default chest. The default chest is the chest that is opened when a user right-clicks a [Chest Keeper] sign. 
/chest rename <old name> <new name>|chestkeeper.use|Changes the specified chest's name to the specified new name. 
/chest upgrade (chest name)|chestkeeper.use|Upgrades the default chest from normal size into large. If a chest name is specified
