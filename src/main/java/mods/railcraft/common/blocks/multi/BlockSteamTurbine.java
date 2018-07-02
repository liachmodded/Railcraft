package mods.railcraft.common.blocks.multi;

import mods.railcraft.common.blocks.multi.TileSteamTurbine.Position;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.Tuple;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 *
 */
public final class BlockSteamTurbine extends BlockMultiBlock {

    public static final PropertyEnum<Position> POSITION = PropertyEnum.create("position", Position.class);
    public static final PropertyEnum<Axis> AXIS = PropertyEnum.create("axis", Axis.class, Axis.X, Axis.Z);

    public BlockSteamTurbine() {
        super(Material.ROCK);
        setDefaultState(getDefaultState().withProperty(AXIS, Axis.Z).withProperty(POSITION, Position.REGULAR));
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POSITION, AXIS);
    }

    @Override
    public TileMultiBlock<?, ?> createTileEntity(World world, IBlockState state) {
        return new TileSteamTurbine();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public Class<TileSteamTurbine> getTileClass(IBlockState state) {
        return TileSteamTurbine.class;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Tuple<Integer, Integer> getTextureDimensions() {
        return new Tuple<>(3, 3);
    }
}
