package com.sil3ntone.disposalchest;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Pratham on 3/29/2017.
 */
public class DisposalChestCommandExecutor implements CommandExecutor {

    private DisposalChest plugin;

    public DisposalChestCommandExecutor(DisposalChest plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player){
            Player p = (Player) commandSender;
            p.sendMessage("§6" + DisposalChest.TAG + ": Place a Sign with text '§Ltrash§r§6' as the topmost line on a chest.");
            int playerIdxInList = DisposalChest.trashChestOwnerLimitList.indexOf(new TrashChestOwner(p.getUniqueId().toString(), -1));
            int currentNoOfTrashChests = 0;
            if(playerIdxInList > -1) {
                currentNoOfTrashChests = DisposalChest.trashChestOwnerLimitList.get(playerIdxInList).getCount();
            }
            p.sendMessage(String.format("§6" + DisposalChest.TAG + ": Your current usage %d/%d chests", currentNoOfTrashChests, DisposalChest.perPlayerChestLimit));
            return true;
        }
        else {
            commandSender.sendMessage(DisposalChest.TAG + ": Place a Sign with text 'trash' as the topmost line on a chest.");
            return true;
        }
    }
}
