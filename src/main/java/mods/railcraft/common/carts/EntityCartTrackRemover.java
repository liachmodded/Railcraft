/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2016
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.carts;

import mods.railcraft.api.carts.CartToolsAPI;
import mods.railcraft.common.blocks.tracks.TrackTools;
import mods.railcraft.common.blocks.tracks.kits.TrackKits;
import mods.railcraft.common.plugins.forge.WorldPlugin;
import mods.railcraft.common.util.misc.Game;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public class EntityCartTrackRemover extends CartBaseMaintenance {

    private final Set<BlockPos> tracksBehind = new HashSet<BlockPos>();
    private final Set<BlockPos> tracksRemoved = new HashSet<BlockPos>();

    public EntityCartTrackRemover(World world) {
        super(world);
    }

    public EntityCartTrackRemover(World world, double d, double d1, double d2) {
        this(world);
        setPosition(d, d1 + getYOffset(), d2);
        motionX = 0.0D;
        motionY = 0.0D;
        motionZ = 0.0D;
        prevPosX = d;
        prevPosY = d1;
        prevPosZ = d2;
    }

    @Override
    public ICartType getCartType() {
        return RailcraftCarts.TRACK_REMOVER;
    }

    @Override
    protected void moveAlongTrack(BlockPos pos, IBlockState state){
        super.moveAlongTrack(pos, state);
        if (Game.isClient(worldObj))
            return;

        for (BlockPos track : tracksBehind) {
            if (track.equals(pos))
                continue;
            removeTrack(track);
        }
        tracksBehind.removeAll(tracksRemoved);
        tracksRemoved.clear();

        addTravelledTrack(pos);
    }

    private void addTravelledTrack(BlockPos pos) {
        tracksBehind.add(pos);
    }

    private void removeTrack(BlockPos track) {
        if (getDistanceSq(track) >= 9)
            tracksRemoved.add(track);
        else if (!TrackTools.isRailBlockAt(worldObj, track))
            tracksRemoved.add(track);
        else if (TrackTools.getTrackKitAt(worldObj, track) == TrackKits.FORCE.getTrackKit())
            tracksRemoved.add(track);
        else if (!CartToolsAPI.isMinecartAt(worldObj, track, -0.2f)) {
            Block block = WorldPlugin.getBlock(worldObj, track);
            removeOldTrack(track, WorldPlugin.getBlockState(worldObj, track));
            blink();
            tracksRemoved.add(track);
        }
    }

}
