package me.zygotecode.amazingmobs.horde;

import me.zygotecode.amazingmobs.mob.ItemSpec;
import me.zygotecode.amazingmobs.util.IntRange;
import me.zygotecode.amazingmobs.util.Rng;
import me.zygotecode.amazingmobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * What participants receive when a horde is cleared: items, XP, console commands (with
 * {@code %player%} substituted), and a message. Runtime grant helper included.
 */
public record HordeRewards(List<ItemSpec> items, IntRange xp, List<String> commands, String message) {

    public static final HordeRewards NONE = new HordeRewards(List.of(), IntRange.of(0), List.of(), null);

    public HordeRewards {
        items = items == null ? List.of() : List.copyOf(items);
        xp = xp == null ? IntRange.of(0) : xp;
        commands = commands == null ? List.of() : List.copyOf(commands);
    }

    public boolean isEmpty() {
        return items.isEmpty() && xp.max() == 0 && commands.isEmpty() && (message == null || message.isBlank());
    }

    public void grant(Player player, Rng rng) {
        for (ItemSpec spec : items) {
            ItemStack stack = spec.build(rng);
            player.getInventory().addItem(stack).values().forEach(left ->
                    player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
        int x = xp.pick(rng);
        if (x > 0) player.giveExp(x);
        for (String cmd : commands) {
            String c = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
        }
        if (message != null && !message.isBlank()) player.sendMessage(Text.mm(message));
    }
}
