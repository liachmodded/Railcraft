/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2017
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.common.modules;

import mods.railcraft.api.core.RailcraftModule;
import mods.railcraft.common.blocks.RailcraftBlocks;
import mods.railcraft.common.blocks.charge.ChargeManager;
import mods.railcraft.common.items.RailcraftItems;
import net.minecraftforge.common.MinecraftForge;

/**
 * @author CovertJaguar <http://www.railcraft.info/>
 */
@RailcraftModule(value = "railcraft:charge", description = "all things charge")
public class ModuleCharge extends RailcraftModulePayload {

    public ModuleCharge() {
        setEnabledEventHandler(new ModuleEventHandler() {
            @Override
            public void construction() {
                MinecraftForge.EVENT_BUS.register(ChargeManager.getEventListener());

                add(
                        RailcraftItems.CHARGE,
                        RailcraftItems.CHARGE_METER,
                        RailcraftItems.TURBINE_ROTOR,
                        RailcraftItems.TURBINE_BLADE,
                        RailcraftItems.TURBINE_DISK,
//                        RailcraftBlocks.track,
                        RailcraftBlocks.TURBINE,
                        RailcraftBlocks.CHARGE_FEEDER,
                        RailcraftBlocks.CHARGE_TRAP,
                        RailcraftBlocks.CHARGE_BATTERY,
                        RailcraftBlocks.FRAME,
                        RailcraftBlocks.WIRE
                );
            }

            @Override
            public void preInit() {
//                EnumMachineAlpha alpha = EnumMachineAlpha.TURBINE;
//                if (alpha.isAvailable()) {
//                    CraftingPlugin.addRecipe(alpha.getStack(3),
//                            "BPB",
//                            "P P",
//                            "BPB",
//                            'P', RailcraftItems.PLATE, Metal.STEEL,
//                            'B', "blockSteel");

//                    CraftingPlugin.addRecipe(new RotorRepairRecipe());

//                ItemStack rotor = RailcraftPartItems.getTurbineRotor();
//                rotor.setItemDamage(25000);
//                CraftingPlugin.addShapelessRecipe(rotor, RailcraftPartItems.getTurbineRotor());
//                }

            }
        });
    }
}
