# DisposalChest
An optimized Bukkit plugin for disposing items fed in manually or by hoppers

## Help command
`/trashchest`

## Notes
1. It doesn't rely on InventoryMoveItemEvent.
1. It clears chests at a fixed interval of 200 ticks (10s) but it dynamically adjusts the number of chests it clears in each batch based on the TPS and total number of trash chests in database.
1. The time it takes to clear all TrashChests on the server is dynamically adjusted as well.

## Config Example 
##### Partial config related to dynamic batch thingy
```
minChestClearTime: 30
maxChestClearTime: 300
minTPS: 10
maxTPS: 20
minChestClearBatchSize: 10
```

## Dynamic batch processing thingy!
```
// Do not do anything if TPS falls below the minimum limit.
if(DisposalChest.tps < DisposalChest.minTPS) {
  return;
}

// Get number of total trash chests
int chestListSize = trashChestList.size();

// If trash chest count is less than one, do nothing!
if(chestListSize < 1) {
  return;
}

// Limit current TPS value to maxTPS (for calculation purpose only)
int limitTPS = Math.min(DisposalChest.maxTPS, Math.round(DisposalChest.tps));

// Calculate the approx time it should take to clear all chests (without considering TPS/realtime thingy)
int timeToClearAllChests = Math.round(DisposalChest.maxChestClearTime - ((DisposalChest.maxChestClearTime - DisposalChest.minChestClearTime) * (DisposalChest.maxTPS - limitTPS) /  (DisposalChest.maxTPS - DisposalChest.minTPS) ));

// As we are clearing a batch every 10 seconds multiply the number of chests it should clear per second by 10
int chestBatchSize = chestListSize / timeToClearAllChests * 10 ;

// Make sure the batch size does not fall below minChestClearBatchSize
chestBatchSize = Math.max(DisposalChest.minChestClearBatchSize, chestBatchSize);
```
