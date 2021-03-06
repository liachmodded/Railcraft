/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2019
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.plugins.forge;

import mods.railcraft.api.core.RailcraftConstantsAPI;
import mods.railcraft.common.core.RailcraftConstants;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static net.minecraft.world.storage.loot.LootTableList.register;

/**
 * A utility class handling stuffs related to loot tables.
 *
 * Created by CovertJaguar on 4/10/2017 for Railcraft.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
//TODO fix loot broken when module is not avaiable. Maybe inject with events?
public final class LootPlugin {
    /**
     * The only instance of loot plugin.
     */
    public static final LootPlugin INSTANCE = new LootPlugin();
    /**
     * The location of the railcraft workshop chest loot table.
     */
    public static final ResourceLocation CHESTS_VILLAGE_WORKSHOP = register(RailcraftConstantsAPI.locationOf( "chests/village_workshop"));
    private static final String[] poolNames = {"tools", "resources", "carts", "tracks", "general"};

    private LootPlugin() {
    }

    /**
     * Initializes the loot plugin.
     */
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void lootLoad(LootTableLoadEvent event) {
        if (!"minecraft".equals(event.getName().getNamespace())) {
            return;
        }

        ResourceLocation resourceLocation = RailcraftConstantsAPI.locationOf(event.getName().getPath());
        LootTable lootTable = event.getLootTableManager().getLootTableFromLocation(resourceLocation); // Causes recursive events, but should be fine
        if (lootTable != null) {
            for (String poolName : poolNames) {
                LootPool pool = lootTable.getPool(RailcraftConstants.RESOURCE_DOMAIN + "_" + poolName);
                if (pool != null)
                    event.getTable().addPool(pool);
            }
        }
    }
}
