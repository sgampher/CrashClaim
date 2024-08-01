package net.crashcraft.crashclaim.data;

import net.crashcraft.crashclaim.CrashClaim;
import net.crashcraft.crashclaim.claimobjects.Claim;
import net.crashcraft.crashclaim.config.GlobalConfig;
import net.crashcraft.crashclaim.payment.TransactionType;
import net.crashcraft.crashclaim.localization.Localization;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class ContributionManager {
    public static int getArea(int minCornerX, int minCornerZ, int maxCornerX, int maxCornerZ){
        return (((maxCornerX + 1) - minCornerX) * ((maxCornerZ + 1) - minCornerZ));
    }

    public static void addContribution(Claim claim, int minCornerX, int minCornerZ, int maxCornerX, int maxCornerZ, UUID player){
        int area = getArea(minCornerX, minCornerZ, maxCornerX, maxCornerZ);
        int originalArea = getArea(claim.getMinX(), claim.getMinZ(), claim.getMaxX(), claim.getMaxZ());

        int difference = area - originalArea;

        if (difference == 0)
            return;

        if (difference > 0){
            claim.addContribution(player, difference);  //add to contribution
        } else {

            refund(claim, Math.abs(difference));
        }
    }

    public static void refundContributors(Claim claim){
        int area = getArea(claim.getMinX(), claim.getMinZ(), claim.getMaxX(), claim.getMaxZ());

        refund(claim, area);
    }

    private static void refund(Claim claim, int multiplier) {
        for (Map.Entry<UUID, Integer> entry : claim.getContribution().entrySet()){
            int value =  (int) Math.floor(Math.floor(multiplier *  GlobalConfig.getCostOfBlock(entry.getKey())) / claim.getContribution().size());
            CrashClaim.getPlugin().getPayment().makeTransaction(entry.getKey(), TransactionType.DEPOSIT, "Claim Refund", value, (transaction) -> {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(transaction.getOwner());
                if (offlinePlayer.isOnline()){
                    Player p = offlinePlayer.getPlayer();
                    if (p != null) {
                        p.spigot().sendMessage(Localization.CONTRIBUTION_REFUND.getMessage(p,
                                "amount", Integer.toString((int) Math.floor(transaction.getAmount()))));
                    }
                }
            });
        }
    }
}
