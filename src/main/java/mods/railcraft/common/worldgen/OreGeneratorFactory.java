/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2019
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.worldgen;

import com.google.common.collect.ImmutableSet;
import mods.railcraft.api.core.RailcraftConstantsAPI;
import mods.railcraft.common.core.RailcraftConfig;
import mods.railcraft.common.util.collections.BlockItemParser;
import mods.railcraft.common.util.misc.Game;
import mods.railcraft.common.util.misc.WhiteBlackList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.Level;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by CovertJaguar on 6/7/2017 for Railcraft.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class OreGeneratorFactory {
    enum Type {
        MINE,
        DIFFUSE
    }

    public static final String CAT = "ore";
    public final Type type;
    public final GeneratorSettings settings;
    public final IWorldGenerator worldGen;

    public static OreGeneratorFactory makeMine(Configuration config, int defaultWeight, int defaultBlockCount, int defaultDepth, int defaultRange, int defaultSeed, String defaultFringeOre, String defaultCoreOre) {
        return new OreGeneratorFactory(config, "MINE", defaultWeight, defaultBlockCount, defaultDepth, defaultRange, defaultSeed, defaultFringeOre, defaultCoreOre);
    }

    public OreGeneratorFactory(Configuration config) {
        this(config, "MINE", 100, 4, 40, 3, 29, "railcraft:ore_metal_poor#2", "railcraft:ore_metal#0");
    }

    private OreGeneratorFactory(Configuration config, String defaultType, int defaultWeight, int defaultBlockCount, int defaultDepth, int defaultRange, int defaultSeed, String defaultFringeOre, String defaultCoreOre) {
        config.setCategoryComment(CAT, "Copy this file to add your own ore spawns or deleted it to disable spawning.\n" +
                "Setting railcraft.config->worldgen.generateDefaultConfigs to true will reset the entire folder to defaults.");
        config.setCategoryComment(CAT + ".retrogen", "Retrogen settings. You must have the Railcraft-Retrogen mod installed for these to do anything.");
        boolean retrogen = config.getBoolean("retrogen", CAT + ".retrogen", false, "Whether retrogen should be enabled on this generator.");
        String retrogenMarker = config.getString("retrogenMarker", CAT + ".retrogen", "RCRGMARK", "The marker used to indicate whether a chunk has generated this ore. Generally this should be unique each time you run retrogen.");

        String name = config.getConfigFile().getName().replace(".cfg", "").replace(" ", "_");

        type = Type.valueOf(config.getString("type", CAT, defaultType, "The generation type, can be either 'DIFFUSE' or 'MINE'."));

        DimensionRules dimensionRules = new DimensionRules(config);
        BiomeRules biomeRules = new BiomeRules(config);

        switch (type) {
            case MINE:
                GeneratorSettingsMine mineSettings = new GeneratorSettingsMine(config, defaultWeight, defaultBlockCount, defaultDepth, defaultRange, defaultSeed, defaultFringeOre, defaultCoreOre);
                this.settings = mineSettings;
                IWorldGenerator mineGenImpl = new GeneratorMine(config, dimensionRules, biomeRules, mineSettings);
                worldGen = new GeneratorRailcraftOre(mineGenImpl, retrogen, retrogenMarker).setRegistryName(RailcraftConstantsAPI.locationOf(name));
                Game.log().msg(Level.INFO, "Registered Mine Ore Generator at depth {0} called {1}", mineSettings.depth, name);
                break;
            case DIFFUSE:
                GeneratorSettings diffuseSettings = new GeneratorSettings(config, defaultWeight, defaultBlockCount, defaultDepth, defaultRange, defaultCoreOre);
                this.settings = diffuseSettings;
                IWorldGenerator diffuseGenImpl = new GeneratorDiffuse(dimensionRules, biomeRules, diffuseSettings);
                worldGen = new GeneratorRailcraftOre(diffuseGenImpl, retrogen, retrogenMarker).setRegistryName(RailcraftConstantsAPI.locationOf(name));
                Game.log().msg(Level.INFO, "Registered Diffuse Ore Generator called {0}", name);
                break;
            default:
                throw new OreConfigurationException(config, "Something went wrong. This should be impossible.");
        }

        if (config.hasChanged())
            config.save();
    }

    public static class GeneratorSettings {
        public final int weight;
        public final int depth;
        public final int range;
        public final int blockCount;
        public final IBlockState coreOre;

        public GeneratorSettings(Configuration config, int defaultWeight, int defaultBlockCount, int defaultDepth, int defaultRange, String defaultCoreOre) {
            weight = config.getInt("weight", CAT, defaultWeight, 0, Integer.MAX_VALUE, "The generator weight, larger weights generate later. You can use this to sort what order stuff is generated.");
            depth = config.getInt("depth", CAT, defaultDepth, 10, Integer.MAX_VALUE, "The y level that the mine will generate at. Generally you should keep this below 220 for vanilla height worlds. If your sea level is the normal 63, its usually best to stay below 50 as well due to the topsoil.");
            range = config.getInt("range", CAT, defaultRange, 1, 20, "The scale of the gaussian distribution used to spread the mine vertically, how tall it is. Note that it spreads above and blow the y level equally, so a value of 3 is roughly 6 blocks tall.");
            blockCount = config.getInt("blockCount", CAT, defaultBlockCount, 1, 16, "The number of ore blocks generated during each successful event. Each chunk generally gets 216 generation events, but not all events result in ore spawn due to chance settings and noise fields.");


            config.setCategoryComment(CAT + ".ore", "The ore blocks to be generated. Format: <modId>:<blockname>#<meta>");
            coreOre = BlockItemParser.parseBlock(config.getString("core", CAT + ".ore", defaultCoreOre, "The ore block generated in the core of the mine.")).stream().findFirst().orElse(Blocks.STONE.getDefaultState());
        }
    }

    public static class GeneratorSettingsMine extends GeneratorSettings {
        public final boolean skyGen;
        public final int noiseSeed;
        public final double cloudScale, veinScale, fringeLimit, richLimit, coreLimit, veinLimit, fringeGenChance, coreGenChance, coreOreChance;
        public final IBlockState fringeOre;

        public GeneratorSettingsMine(Configuration config, int defaultWeight, int defaultBlockCount, int defaultDepth, int defaultRange, int defaultSeed, String defaultFringeOre, String defaultCoreOre) {
            super(config, defaultWeight, defaultBlockCount, defaultDepth, defaultRange, defaultCoreOre);

            this.skyGen = RailcraftConfig.isWorldGenEnabled("sky");

            config.setCategoryComment(CAT + ".ore", "The ore blocks to be generated. Format: <modId>:<blockname>#<meta>");
            fringeOre = BlockItemParser.parseBlock(config.getString("fringe", CAT + ".ore", defaultFringeOre, "The ore block generated on the fringe of the mine.")).stream().findFirst().orElse(Blocks.STONE.getDefaultState());

            noiseSeed = config.getInt("seed", CAT, defaultSeed, 0, Integer.MAX_VALUE, "The seed used to create the noise map. Generally it is set to the atomic number of the element being generated, but it can be anything you want. Should be unique for each generator or your mines will generate in the same places, which can be desirable if you want to mix ores like Iron and Nickel.");
            cloudScale = config.getFloat("cloud", CAT + ".scale", 0.0018F, 0.000001F, 1F, "The scale of the noise map used to determine the boundaries of the mine. Very small changes can have drastic effects. Smaller numbers result in larger mines. Recommended to not change this.");
            veinScale = config.getFloat("vein", CAT + ".scale", 0.015F, 0.000001F, 1F, "The scale of the noise map used to create the veins. Very small changes can have drastic effects. Smaller numbers result in larger veins. Recommended to not change this.");
            fringeLimit = config.getFloat("fringe", CAT + ".limits", 0.7F, 0F, 1F, "The limit of noise of the cloud layer above which fringe ore is generated. Lower numbers result in larger, more common, fringe areas.");
            richLimit = config.getFloat("rich", CAT + ".limits", 0.8F, 0F, 1F, "The limit of noise of the cloud layer above which core ore is generated in rich biomes. Lower numbers result in larger rich areas.");
            coreLimit = config.getFloat("core", CAT + ".limits", 0.9F, 0F, 1F, "The limit of noise of the cloud layer above which core ore is generated. Lower numbers result in larger core areas.");
            veinLimit = config.getFloat("vein", CAT + ".limits", 0.25F, 0F, 1F, "The limit of noise of the vein layer below which ore is generated. Larger numbers result in larger veins.");
            fringeGenChance = config.getFloat("fringeGen", CAT + ".chances", 0.3F, 0F, 1F, "The percent chance that a generate event in a fringe area will result in ore spawning.");
            coreGenChance = config.getFloat("coreGen", CAT + ".chances", 1F, 0F, 1F, "The percent chance that a generate event in a core area will result in ore spawning.");
            coreOreChance = config.getFloat("coreOre", CAT + ".chances", 0.2F, 0F, 1F, "The percent chance that a generate event in a core area will result in core ore spawning instead of fringe ore. Applied after coreGen.");
        }
    }

    public static class DimensionRules {
        final Set<String> worldProviderBlacklist;
        final Set<Integer> dimensionBlacklist;
        final Set<Integer> dimensionWhitelist;

        public DimensionRules(Configuration config) {
            config.setCategoryComment(CAT + ".dimensions", "Control which dimensions the generator is allowed to generate in.\n" +
                    "Generally they still require stone to generate in regardless, though this may become a config option in the future.\n" +
                    "The system is permissive and will allow any dimension not blacklisted to be generated in.");
            String[] worldProvidersNames = config.getStringList("worldProvidersBlacklist", CAT + ".dimensions", new String[]{"net.minecraft.world.WorldProviderHell", "net.minecraft.world.WorldProviderEnd"}, "World Provider classes to disallow generation in.");
            worldProviderBlacklist = Collections.unmodifiableSet(Arrays.stream(worldProvidersNames).collect(Collectors.toSet()));

            String[] dimensionIds = config.getStringList("dimensionBlacklist", CAT + ".dimensions", new String[]{"-1", "1"}, "Dimension IDs to disallow generation in.");
            dimensionBlacklist = Collections.unmodifiableSet(Arrays.stream(dimensionIds).map(Integer::parseInt).collect(Collectors.toSet()));

            dimensionIds = config.getStringList("dimensionWhitelist", CAT + ".dimensions", new String[]{"0"}, "Dimension IDs to allow generation in. Overrides blacklists.");
            dimensionWhitelist = Collections.unmodifiableSet(Arrays.stream(dimensionIds).map(Integer::parseInt).collect(Collectors.toSet()));
        }

        public boolean isDimensionValid(World world) {
            if (dimensionWhitelist.contains(world.provider.getDimension()))
                return true;
            return !dimensionBlacklist.contains(world.provider.getDimension()) && !worldProviderBlacklist.contains(world.provider.getClass().getName());
        }
    }

    public static final class BiomeRules {
        final WhiteBlackList<Biome> validBiomes;
        final WhiteBlackList<BiomeDictionary.Type> validBiomeTypes;
        final WhiteBlackList<Biome> richBiomes;
        final WhiteBlackList<BiomeDictionary.Type> richBiomeTypes;

        public BiomeRules(Configuration config) {

            Function<String, Biome> biomeFactory = (biomeId) -> ForgeRegistries.BIOMES.getValue(new ResourceLocation(biomeId));

            config.setCategoryComment(CAT + ".biomes", "Expects fully qualified Biome registry names.\n" +
                    "See Biome.java in Minecraft/Forge for the names.\n" +
                    "Format: <modId>:<biome_registry_name>.\n" +
                    "'<modId>:all' can be used to specify all Biomes from a specific mod.");
            String[] biomeBlacklistNames = config.getStringList("blacklist", CAT + ".biomes", new String[]{}, "Biome registry names where the ore will will not generate. Takes priority over the whitelist and types.");
//            biomeBlacklist = Collections.unmodifiableSet(Arrays.stream(biomeBlacklistNames).flatMap(this::getBiomes).collect(Collectors.toSet()));
            String[] biomeWhitelistNames = config.getStringList("whitelist", CAT + ".biomes", new String[]{}, "Biome registry names where the ore will generate. Takes priority over types.");
//            biomeWhitelist = Collections.unmodifiableSet(Arrays.stream(biomeWhitelistNames).flatMap(this::getBiomes).collect(Collectors.toSet()));

            validBiomes = makeDoubleList(biomeFactory, biomeBlacklistNames, biomeWhitelistNames);

            config.setCategoryComment(CAT + ".biomesTypes", "Biome Dictionary types can be found in BiomeDictionary.java in Forge.\n" +
                    "You can use 'ALL' to specify all types.");
            String[] biomeTypeBlacklistNames = config.getStringList("blacklist", CAT + ".biomesTypes", new String[]{}, "Biome Dictionary types where the ore will will not generate. Takes priority over the whitelist.");
//            biomeTypeBlacklist = Collections.unmodifiableSet(Arrays.stream(biomeTypeBlacklistNames).flatMap(this::getTypes).collect(Collectors.toSet()));
            String[] biomeTypeWhitelistNames = config.getStringList("whitelist", CAT + ".biomesTypes", new String[]{"ALL"}, "Biome Dictionary types where the ore will generate.");
//            biomeTypeWhitelist = Collections.unmodifiableSet(Arrays.stream(biomeTypeWhitelistNames).flatMap(this::getTypes).collect(Collectors.toSet()));

            validBiomeTypes = makeDoubleList(BiomeDictionary.Type::getType, biomeTypeBlacklistNames, biomeTypeWhitelistNames);

            config.setCategoryComment(CAT + ".rich", "Biomes where the ore will generator more richly.");
            String[] richBiomeNames = config.getStringList("biomes", CAT + ".rich", new String[]{"minecraft:mesa"}, "Biomes where the ore will generator more richly. Expects fully qualified Biome registry names. '<modId>:all' can be used to specify all Biomes from a specific mod.");
            richBiomes = makeSingleList(biomeFactory, richBiomeNames);

            String[] richBiomeTypeNames = config.getStringList("biomeTypes", CAT + ".rich", new String[]{"MOUNTAIN", "MESA", "HILLS"}, "Biome Dictionary types where the ore will generator more richly. You can use 'ALL' to specify all types.");
            richBiomeTypes = makeSingleList(BiomeDictionary.Type::getType, richBiomeTypeNames);

        }

        public boolean isValidBiome(Biome biome) {
            WhiteBlackList.PermissionLevel level = validBiomes.getPermissionLevel(biome);

            if (level != WhiteBlackList.PermissionLevel.DEFAULT)
                return level == WhiteBlackList.PermissionLevel.WHITELISTED;

            // FIXME this is wrong, it doesn't have to match all
            return BiomeDictionary.getTypes(biome).stream().allMatch(validBiomeTypes::permits);
        }

        public boolean isRichBiome(Biome biome) {
            WhiteBlackList.PermissionLevel level = richBiomes.getPermissionLevel(biome);

            if (level != WhiteBlackList.PermissionLevel.DEFAULT)
                return level == WhiteBlackList.PermissionLevel.WHITELISTED;

            // FIXME this is wrong, it doesn't have to match all
            return BiomeDictionary.getTypes(biome).stream().anyMatch(richBiomeTypes::permits);
        }

//        private Stream<Biome> getBiomes(String name) {
//            ResourceLocation resource = new ResourceLocation(name);
//            if ("all".equalsIgnoreCase(resource.getPath()))
//                return StreamSupport.stream(Biome.REGISTRY.spliterator(), false).filter(b -> resource.getNamespace().equalsIgnoreCase(b.getRegistryName().getNamespace()));
//            Biome biome = Biome.REGISTRY.getObject(resource);
//            if (biome == null)
//                return Stream.empty();
//            return Stream.of(biome);
//        }
//
//        private Stream<BiomeDictionary.Type> getTypes(String name) {
//            name = name.toUpperCase(Locale.ROOT);
//            if ("ALL".equalsIgnoreCase(name)) {
//                noWhiteList = true;
//                return Stream.empty();
//            }
//            return Stream.of(BiomeDictionary.Type.getType(name));
//        }

        private static <E> WhiteBlackList<E> makeSingleList(Function<String, E> transform, String[] whiteEntries) {
            return makeDoubleList(transform, new String[0], whiteEntries);
        }

        private static <E> WhiteBlackList<E> makeDoubleList(Function<String, E> transform, String[] blackEntries, String[] whiteEntries) {
            ImmutableSet.Builder<E> blackListBuilder = ImmutableSet.builder();
            for (String blackEntry : blackEntries) {
                E result = transform.apply(blackEntry);
                if (result != null) {
                    blackListBuilder.add(result);
                }
            }

            ImmutableSet<E> blackList = blackListBuilder.build();

            boolean whiteListDisabled = false;
            ImmutableSet.Builder<E> whiteListBuilder = ImmutableSet.builder();
            for (String whiteEntry : whiteEntries) {
                if ("all".equalsIgnoreCase(whiteEntry)) {
                    whiteListDisabled = true;
                    break;
                }
                E result = transform.apply(whiteEntry);
                if (result != null) {
                    whiteListBuilder.add(result);
                }
            }

            return whiteListDisabled ? WhiteBlackList.create(blackList) : WhiteBlackList.create(blackList, whiteListBuilder.build());
        }
    }

    /**
     * Created by CovertJaguar on 6/9/2017 for Railcraft.
     *
     * @author CovertJaguar <http://www.railcraft.info>
     */
    public static class OreConfigurationException extends RuntimeException {
        public OreConfigurationException(Configuration config, String msg) {
            super("Error detected in Ore Config: " + config.getConfigFile().getName() + " - " + msg);
        }
    }
}
