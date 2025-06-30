package net.denfry.phantomVotePlugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class PhantomKiller {
    public static void killAllPhantoms() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getType() == EntityType.PHANTOM) {
                    entity.remove();
                }
            }
        }
    }
}